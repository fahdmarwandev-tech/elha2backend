package com.game3alhaq.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class BookingResponses {

    private BookingResponses() {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingResponse {
        private Long id;
        private String code;
        private Long hallId;
        private String hallName;
        private Long userId;
        private String fullName;
        private String nationalId;
        private String phone;
        private String secondaryPhone;
        private String email;
        private String bookingType;
        private String eventLabel;
        private LocalDate eventDate;
        private LocalDate endDate;
        private List<LocalDate> reservedDates;
        private Integer daysCount;
        private Integer startHour;
        private Integer endHour;
        private Integer durationHours;
        private String formattedTimeRange;
        private Integer appliedBufferHours;
        private BigDecimal hourlyRate;
        private BigDecimal totalAmount;
        private String currency;
        private String notes;
        private String status;
        private OffsetDateTime paymentExpiresAt;
        private Long paymentId;
        private String paypalOrderId;
        private String paymentStatus;
        private OffsetDateTime createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BookingInitiationResponse {
        private BookingResponse booking;
        private String paypalOrderId;
        private String approvalUrl;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentResponse {
        private Long id;
        private Long bookingId;
        private String provider;
        private String paypalOrderId;
        private String paypalCaptureId;
        private String payerEmail;
        private BigDecimal amount;
        private String currency;
        private String status;
        private OffsetDateTime paidAt;
        private OffsetDateTime createdAt;
    }
}
