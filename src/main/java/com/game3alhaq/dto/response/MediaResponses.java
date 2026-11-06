package com.game3alhaq.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

public class MediaResponses {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoResponse {
        private Long id;
        private String videoSourceType; // "YOUTUBE" or "LOCAL"
        private String youtubeUrl;
        private String youtubeId;
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
        private OffsetDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String slug;
        private String description;
        private Integer sortOrder;
        private Boolean isActive;
        private OffsetDateTime createdAt;
    }
}
