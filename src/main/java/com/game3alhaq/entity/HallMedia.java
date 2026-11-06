package com.game3alhaq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "hall_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "media_url", nullable = false, length = 500)
    private String mediaUrl;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "subtitle", length = 255)
    private String subtitle;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
