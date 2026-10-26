package com.munot.hospital.controller;

import com.munot.hospital.dto.ChatDtos.*;
import com.munot.hospital.security.CurrentUser;
import com.munot.hospital.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@PreAuthorize("hasAnyRole('DOCTOR','RECEPTIONIST')")
public class ChatController {
    private final ChatService chat;

    public ChatController(ChatService chat) { this.chat = chat; }

    @GetMapping("/messages")
    public List<ChatMessageView> messages(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
                                          @RequestParam(required = false) Long before) {
        return chat.list(CurrentUser.get(), since, before);
    }

    @PostMapping("/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageView send(@Valid @RequestBody SendRequest req) { return chat.send(CurrentUser.get(), req.body()); }

    @DeleteMapping("/messages/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { chat.delete(CurrentUser.get(), id); }

    @GetMapping("/unread")
    public Map<String, Long> unread() { return Map.of("count", chat.unread(CurrentUser.get())); }

    @PutMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void read() { chat.markRead(CurrentUser.get()); }

    @GetMapping("/members")
    public List<ChatMember> members() { return chat.members(CurrentUser.get()); }
}
