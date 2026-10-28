package com.game3alhaq.mapper;

import com.game3alhaq.dto.response.MediaResponses.VideoResponse;
import com.game3alhaq.entity.Video;
import org.springframework.stereotype.Component;

@Component
public class VideoMapper {

    public VideoResponse toResponse(Video video) {
        if (video == null) return null;

        return VideoResponse.builder()
                .id(video.getId())
                .videoSourceType(video.getVideoSourceType())
                .youtubeUrl(video.getYoutubeUrl())
                .youtubeId(video.getYoutubeId())
                .fileUrl(video.getFileUrl())
                .fileName(video.getFileName())
                .fileSize(video.getFileSize())
                .title(video.getTitle())
                .description(video.getDescription())
                .category(video.getCategory())
                .categoryName(video.getCategoryName())
                .duration(video.getDuration())
                .viewsCount(video.getViewsCount())
                .customPoster(video.getCustomPoster())
                .isFeatured(video.getIsFeatured())
                .createdAt(video.getCreatedAt())
                .build();
    }
}
