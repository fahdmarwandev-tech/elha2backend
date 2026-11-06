package com.game3alhaq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

public final class BookingRequests {

    private BookingRequests() {}

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateBookingRequest {
        @NotNull(message = "Hall ID is required")
        private Long hallId;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "National ID is required")
        private String nationalId;

        @NotBlank(message = "Phone number is required")
        private String phone;

        private String secondaryPhone;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Booking type is required")
        private String bookingType;

        @NotNull(message = "Event date is required")
        private LocalDate eventDate;

        @NotNull(message = "Start hour is required")
        private Integer startHour;

        @NotNull(message = "End hour is required")
        private Integer endHour;

        private String notes;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminCreateBookingRequest {
        @NotNull(message = "Hall ID is required")
        private Long hallId;

        @NotBlank(message = "Full name is required")
        private String fullName;

        @NotBlank(message = "National ID is required")
        private String nationalId;

        @NotBlank(message = "Phone number is required")
        private String phone;

        private String secondaryPhone;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Booking type is required")
        private String bookingType;

        @NotNull(message = "Event date is required")
        private LocalDate eventDate;

        @NotNull(message = "Start hour is required")
        private Integer startHour;

        @NotNull(message = "End hour is required")
        private Integer endHour;

        private String notes;

        private Boolean skipPayment;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateBookingStatusRequest {
        @NotBlank(message = "Status is required")
        private String status;
    }
}
