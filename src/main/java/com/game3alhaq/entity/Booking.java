package com.game3alhaq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hall_id", nullable = false)
    private Hall hall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "national_id", length = 50)
    private String nationalId;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "secondary_phone", length = 30)
    private String secondaryPhone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "booking_type", nullable = false, length = 50)
    private String bookingType;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "start_hour", nullable = false)
    private Integer startHour;

    @Column(name = "end_hour", nullable = false)
    private Integer endHour;

    @Column(name = "duration_hours", nullable = false)
    private Integer durationHours;

    @Column(name = "applied_buffer_hours", nullable = false)
    @Builder.Default
    private Integer appliedBufferHours = 1;

    @Column(name = "hourly_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "OMR";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "PENDING_PAYMENT";

    @Column(name = "payment_expires_at")
    private OffsetDateTime paymentExpiresAt;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
