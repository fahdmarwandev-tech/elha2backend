package com.game3alhaq.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

public class HallRequests {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateHallConfigRequest {
        @NotBlank(message = "Hall name is required")
        private String name;

        private String description;

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        private Integer capacity;

        @NotNull(message = "Hourly rate is required")
        @Positive(message = "Hourly rate must be positive")
        private BigDecimal hourlyRate;

        private String currency;

        @NotNull(message = "Buffer hours is required")
        @Min(value = 0, message = "Buffer hours must be between 0 and 5")
        @Max(value = 5, message = "Buffer hours must be between 0 and 5")
        private Integer bufferHours;

        @NotNull(message = "Open hour is required")
        @Min(value = 0, message = "Open hour must be between 0 and 23")
        @Max(value = 23, message = "Open hour must be between 0 and 23")
        private Integer openHour;

        @NotNull(message = "Close hour is required")
        @Min(value = 1, message = "Close hour must be between 1 and 24")
        @Max(value = 24, message = "Close hour must be between 1 and 24")
        private Integer closeHour;

        private String featuresJson;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateBlackoutRequest {
        @NotNull(message = "Hall ID is required")
        private Long hallId;

        @NotNull(message = "Blackout date is required")
        private LocalDate blackoutDate;

        @Min(value = 0, message = "Start hour must be between 0 and 23")
        @Max(value = 23, message = "Start hour must be between 0 and 23")
        private Integer startHour;

        @Min(value = 1, message = "End hour must be between 1 and 24")
        @Max(value = 24, message = "End hour must be between 1 and 24")
        private Integer endHour;

        private String reason;
    }
}
