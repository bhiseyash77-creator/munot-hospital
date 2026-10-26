package com.munot.hospital.repository;

import com.munot.hospital.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByOrderByIdDesc();
    List<ChatMessage> findTop50ByIdLessThanOrderByIdDesc(Long beforeId);
    List<ChatMessage> findTop200ByUpdatedAtGreaterThanEqualOrderByIdAsc(LocalDateTime since);
    long countByIdGreaterThanAndSenderIdNotAndDeletedFalse(Long id, Long senderId);
    long countBySenderIdAndCreatedAtAfter(Long senderId, LocalDateTime after);
    ChatMessage findTopByOrderByIdDesc();
}
