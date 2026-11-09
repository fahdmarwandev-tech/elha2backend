package com.game3alhaq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

public class StaffEmailResponses {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffEmailResponse {
        private Long id;
        private String fullName;
        private String email;
        private OffsetDateTime createdAt;
    }
}
