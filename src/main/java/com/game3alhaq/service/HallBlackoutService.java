package com.game3alhaq.service;

import com.game3alhaq.dto.request.HallRequests.CreateBlackoutRequest;
import com.game3alhaq.dto.response.HallResponses.BlackoutResponse;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.entity.HallBlackout;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.HallMapper;
import com.game3alhaq.repository.HallBlackoutRepository;
import com.game3alhaq.repository.HallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HallBlackoutService {

    private final HallBlackoutRepository blackoutRepository;
    private final HallRepository hallRepository;
    private final HallMapper hallMapper;

    @Transactional(readOnly = true)
    public List<BlackoutResponse> getBlackoutsByHall(Long hallId) {
        List<HallBlackout> list = (hallId == null)
                ? blackoutRepository.findAllByOrderByBlackoutDateAsc()
                : blackoutRepository.findAllByHallIdOrderByBlackoutDateAsc(hallId);
        return list.stream()
                .map(hallMapper::toBlackoutResponse)
                .toList();
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BlackoutResponse createBlackout(CreateBlackoutRequest request) {
        validateHours(request.getStartHour(), request.getEndHour());
        Hall hall = hallRepository.findById(request.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall", "id", request.getHallId()));

        HallBlackout blackout = HallBlackout.builder()
                .hall(hall)
                .blackoutDate(request.getBlackoutDate())
                .startHour(request.getStartHour())
                .endHour(request.getEndHour())
                .reason(request.getReason())
                .build();

        return hallMapper.toBlackoutResponse(blackoutRepository.save(blackout));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBlackout(Long id) {
        if (!blackoutRepository.existsById(id)) {
            throw new ResourceNotFoundException("HallBlackout", "id", id);
        }
        blackoutRepository.deleteById(id);
    }

    private void validateHours(Integer startHour, Integer endHour) {
        if (startHour != null && endHour != null && startHour >= endHour) {
            throw new IllegalArgumentException("End hour must be greater than start hour");
        }
    }
}
