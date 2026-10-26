package com.munot.hospital.dto;

import com.munot.hospital.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public final class ChatDtos {
    private ChatDtos() {}

    public record SendRequest(@NotBlank @Size(max = 1000) String body) {}

    /** Deleted messages keep their place in the thread but carry no text. */
    public record ChatMessageView(Long id, Long senderId, String senderName, Role senderRole, String body, boolean deleted,
                                  LocalDateTime createdAt, LocalDateTime updatedAt, boolean mine) {}

    public record ChatMember(Long id, String name, Role role) {}
}
