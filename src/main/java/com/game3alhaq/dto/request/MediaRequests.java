package com.game3alhaq.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class MediaRequests {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateVideoRequest {

        @NotBlank(message = "YouTube URL is required")
        private String youtubeUrl;

        private String title;
        private String description;
        private String category;
        private String categoryName;
        private String duration;
        private String viewsCount;
        private String customPoster;
        private Boolean isFeatured;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateVideoRequest {
        private String title;
        private String description;
        private String category;
        private String categoryName;
        private String duration;
        private String viewsCount;
        private String customPoster;
        private Boolean isFeatured;
    }
}
