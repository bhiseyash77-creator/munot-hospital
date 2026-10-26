package com.munot.hospital.service;

import com.munot.hospital.common.ApiException;
import com.munot.hospital.dto.ChatDtos.*;
import com.munot.hospital.entity.*;
import com.munot.hospital.repository.*;
import com.munot.hospital.util.ChatRules;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** One shared channel where doctors and receptionists can message each other and see the whole conversation. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {
    private final ChatMessageRepository messages;
    private final ChatReadRepository reads;
    private final UserRepository users;
    private final AuditService audit;

    private static void requireStaff(User u) {
        if (!ChatRules.canUse(u.getRole())) throw ApiException.forbidden("Chat is for doctors and reception only");
    }

    private ChatMessageView view(ChatMessage m, User viewer) {
        return new ChatMessageView(m.getId(), m.getSender().getId(), m.getSender().getFullName(), m.getSender().getRole(),
                m.isDeleted() ? "" : m.getBody(), m.isDeleted(), m.getCreatedAt(), m.getUpdatedAt(), m.getSender().getId().equals(viewer.getId()));
    }

    /** since -> messages created or changed (e.g. deleted) at/after that moment; before -> older page; neither -> latest 50. Oldest first. */
    public List<ChatMessageView> list(User actor, LocalDateTime since, Long before) {
        requireStaff(actor);
        List<ChatMessage> list;
        if (since != null) {
            list = messages.findTop200ByUpdatedAtGreaterThanEqualOrderByIdAsc(since);
        } else {
            list = new ArrayList<>(before != null ? messages.findTop50ByIdLessThanOrderByIdDesc(before) : messages.findTop50ByOrderByIdDesc());
            Collections.reverse(list);
        }
        return list.stream().map(m -> view(m, actor)).toList();
    }

    @Transactional
    public ChatMessageView send(User actor, String body) {
        requireStaff(actor);
        String text;
        try {
            text = ChatRules.normalize(body);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest(e.getMessage());
        }
        if (messages.countBySenderIdAndCreatedAtAfter(actor.getId(), LocalDateTime.now().minusMinutes(1)) >= ChatRules.MAX_PER_MINUTE) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "You are sending messages too fast. Please wait a moment.");
        }
        ChatMessage m = new ChatMessage();
        m.setSender(users.getReferenceById(actor.getId()));
        m.setBody(text);
        m = messages.saveAndFlush(m);
        // your own message never counts as unread for you
        markReadUpTo(actor, m.getId());
        return view(m, actor);
    }

    public long unread(User actor) {
        requireStaff(actor);
        long last = reads.findByUserId(actor.getId()).map(ChatRead::getLastReadMessageId).orElse(0L);
        return messages.countByIdGreaterThanAndSenderIdNotAndDeletedFalse(last, actor.getId());
    }

    @Transactional
    public void markRead(User actor) {
        requireStaff(actor);
        ChatMessage latest = messages.findTopByOrderByIdDesc();
        if (latest != null) markReadUpTo(actor, latest.getId());
    }

    private void markReadUpTo(User actor, long id) {
        ChatRead r = reads.findByUserId(actor.getId()).orElseGet(() -> { ChatRead n = new ChatRead(); n.setUser(users.getReferenceById(actor.getId())); return n; });
        if (id > r.getLastReadMessageId()) { r.setLastReadMessageId(id); reads.save(r); }
    }

    /** A sender can delete only their own message; the text is erased, the placeholder stays. */
    @Transactional
    public void delete(User actor, Long id) {
        requireStaff(actor);
        ChatMessage m = messages.findById(id).orElseThrow(() -> ApiException.notFound("Message not found"));
        if (!m.getSender().getId().equals(actor.getId())) throw ApiException.forbidden("You can delete only your own messages");
        m.setDeleted(true);
        m.setBody("");
        messages.save(m);
        audit.log("CHAT_MESSAGE_DELETED", "ChatMessage", id, null);
    }

    public List<ChatMember> members(User actor) {
        requireStaff(actor);
        List<ChatMember> out = new ArrayList<>();
        for (Role r : List.of(Role.DOCTOR, Role.RECEPTIONIST))
            users.findByRoleOrderByFullName(r).stream().filter(User::isEnabled).forEach(u -> out.add(new ChatMember(u.getId(), u.getFullName(), u.getRole())));
        return out;
    }
}
