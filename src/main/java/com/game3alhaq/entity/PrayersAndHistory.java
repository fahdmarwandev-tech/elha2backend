package com.game3alhaq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "prayers_and_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrayersAndHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fajr_time", length = 20)
    private String fajrTime;

    @Column(name = "sunrise_time", length = 20)
    private String sunriseTime;

    @Column(name = "dhuhr_time", length = 20)
    private String dhuhrTime;

    @Column(name = "asr_time", length = 20)
    private String asrTime;

    @Column(name = "maghrib_time", length = 20)
    private String maghribTime;

    @Column(name = "isha_time", length = 20)
    private String ishaTime;

    @Column(name = "manual_override")
    @Builder.Default
    private Boolean manualOverride = false;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
