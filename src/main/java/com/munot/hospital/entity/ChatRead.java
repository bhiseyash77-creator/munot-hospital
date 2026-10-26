package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "chat_reads")
public class ChatRead extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private User user;
    @Column(name = "last_read_message_id", nullable = false) private long lastReadMessageId;
}
