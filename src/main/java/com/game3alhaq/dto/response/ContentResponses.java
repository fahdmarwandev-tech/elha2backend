package com.game3alhaq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class ContentResponses {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PrayerTimesResponse {
        private String fajr;
        private String sunrise;
        private String dhuhr;
        private String asr;
        private String maghrib;
        private String isha;
        private Boolean manualOverride;
        private LocalDate effectiveDate;
        private String nextPrayer;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryResponse {
        private String mosqueName;
        private String location;
        private Integer inaugurationYear;
        private String landArea;
        private Integer worshippersCapacity;
        private Integer majlisCapacity;
        private String minarets;
        private Integer gatesCount;
        private String overview;
        private List<String> facilities;
        private List<String> waqfProjects;
    }
}
