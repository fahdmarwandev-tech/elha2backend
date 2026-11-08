package com.game3alhaq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "halls")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", unique = true, nullable = false, length = 200)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "OMR";

    @Column(name = "buffer_hours", nullable = false)
    @Builder.Default
    private Integer bufferHours = 1;

    @Column(name = "open_hour", nullable = false)
    @Builder.Default
    private Integer openHour = 0;

    @Column(name = "close_hour", nullable = false)
    @Builder.Default
    private Integer closeHour = 24;

    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @OneToMany(mappedBy = "hall", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<HallMedia> mediaList = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
