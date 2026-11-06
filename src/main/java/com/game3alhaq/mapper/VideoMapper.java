package com.game3alhaq.mapper;

import com.game3alhaq.dto.response.MediaResponses.VideoResponse;
import com.game3alhaq.entity.Video;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VideoMapper {

    VideoResponse toResponse(Video video);
}
