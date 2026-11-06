package com.game3alhaq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ContentRequests {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrayerTimesUpdateRequest {
        @NotBlank(message = "Fajr time is required")
        private String fajr;

        @NotBlank(message = "Sunrise time is required")
        private String sunrise;

        @NotBlank(message = "Dhuhr time is required")
        private String dhuhr;

        @NotBlank(message = "Asr time is required")
        private String asr;

        @NotBlank(message = "Maghrib time is required")
        private String maghrib;

        @NotBlank(message = "Isha time is required")
        private String isha;

        private Boolean manualOverride;
    }
}
