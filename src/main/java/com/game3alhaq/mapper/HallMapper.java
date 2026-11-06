package com.game3alhaq.mapper;

import com.game3alhaq.dto.response.HallResponses.BlackoutResponse;
import com.game3alhaq.dto.response.HallResponses.HallMediaResponse;
import com.game3alhaq.dto.response.HallResponses.HallResponse;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.entity.HallBlackout;
import com.game3alhaq.entity.HallMedia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HallMapper {

    HallResponse toResponse(Hall hall);

    HallMediaResponse toMediaResponse(HallMedia media);

    @Mapping(source = "hall.id", target = "hallId")
    BlackoutResponse toBlackoutResponse(HallBlackout blackout);
}
