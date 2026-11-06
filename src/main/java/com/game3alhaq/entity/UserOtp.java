package com.game3alhaq.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_otps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "identifier", nullable = false, length = 150)
    private String identifier;

    @Column(name = "otp_code", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "purpose", nullable = false, length = 50)
    @Builder.Default
    private String purpose = "PASSWORD_RESET";

    @Column(name = "expiry_date", nullable = false)
    private OffsetDateTime expiryDate;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
