package com.game3alhaq.mapper;

import com.game3alhaq.dto.request.MediaRequests.CreateCategoryRequest;
import com.game3alhaq.dto.response.MediaResponses.CategoryResponse;
import com.game3alhaq.entity.MediaCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MediaCategoryMapper {

    CategoryResponse toResponse(MediaCategory category);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MediaCategory toEntity(CreateCategoryRequest request);
}
