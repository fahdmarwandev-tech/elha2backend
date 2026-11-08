package com.game3alhaq.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class HallResponses {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HallResponse {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private Integer capacity;
        private BigDecimal hourlyRate;
        private String currency;
        private Integer bufferHours;
        private Integer openHour;
        private Integer closeHour;
        private String featuresJson;
        private List<HallMediaResponse> mediaList;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HallMediaResponse {
        private Long id;
        private String mediaUrl;
        private String title;
        private String subtitle;
        private Integer sortOrder;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlackoutResponse {
        private Long id;
        private Long hallId;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate blackoutDate;
        private Integer startHour;
        private Integer endHour;
        private String reason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyAvailabilityResponse {
        private Long hallId;
        private String hallName;
        private Integer year;
        private Integer month;
        private Integer bufferHours;
        private Integer openHour;
        private Integer closeHour;
        private Integer maghribHour;
        private String maghribTime;
        private List<DayAvailabilityResponse> days;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayAvailabilityResponse {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate date;
        private Integer dayNumber;
        private String status; // AVAILABLE, PARTIALLY_BOOKED, FULLY_BOOKED
        private List<HourSlotStatus> hours;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourSlotStatus {
        private Integer hour;
        private String formattedHour;
        private String status; // AVAILABLE, BOOKED, BUFFER, BLACKOUT
    }
}
