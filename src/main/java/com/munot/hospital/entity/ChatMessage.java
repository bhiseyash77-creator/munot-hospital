package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chat_messages")
public class ChatMessage extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "sender_id") private User sender;
    @Column(nullable = false, length = 1000) private String body;
    @Column(nullable = false) private boolean deleted;
}
