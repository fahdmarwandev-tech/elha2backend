package com.game3alhaq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "provider", nullable = false, length = 50)
    @Builder.Default
    private String provider = "PAYPAL";

    @Column(name = "paypal_order_id", unique = true, nullable = false, length = 100)
    private String paypalOrderId;

    @Column(name = "paypal_capture_id", length = 100)
    private String paypalCaptureId;

    @Column(name = "payer_email", length = 150)
    private String payerEmail;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private String status = "CREATED";

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Transient
    private String approvalUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
