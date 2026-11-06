package com.game3alhaq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "hall_blackouts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HallBlackout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @Column(name = "blackout_date", nullable = false)
    private LocalDate blackoutDate;

    @Column(name = "start_hour")
    private Integer startHour;

    @Column(name = "end_hour")
    private Integer endHour;

    @Column(name = "reason", length = 500)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
