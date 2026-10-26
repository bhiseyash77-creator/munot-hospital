package com.munot.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "content_items")
public class ContentItem extends BaseEntity {
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ContentType type;
    @Column(length = 120) private String slug;
    @Column(nullable = false, length = 200) private String title;
    @Column(length = 200) private String subtitle;
    @Column(columnDefinition = "TEXT") private String body;
    @Column(length = 40) private String icon;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean active = true;
}
