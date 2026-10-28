package com.game3alhaq.dto.request;

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
        @Builder.Default
        private String videoSourceType = "YOUTUBE"; // "YOUTUBE" or "LOCAL"

        private String youtubeUrl;
        private String fileUrl;
        private String fileName;
        private Long fileSize;

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
