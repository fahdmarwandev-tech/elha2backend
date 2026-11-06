package com.game3alhaq.service;

import com.game3alhaq.dto.request.HallRequests.UpdateHallConfigRequest;
import com.game3alhaq.dto.response.HallResponses.HallResponse;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.HallMapper;
import com.game3alhaq.repository.HallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HallService {

    private final HallRepository hallRepository;
    private final HallMapper hallMapper;

    @Transactional(readOnly = true)
    public List<HallResponse> getActiveHalls() {
        return hallRepository.findAllActiveWithMedia().stream()
                .map(hallMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public HallResponse getHallById(Long id) {
        Hall hall = hallRepository.findByIdWithMedia(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", "id", id));
        return hallMapper.toResponse(hall);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public HallResponse updateHallConfig(Long id, UpdateHallConfigRequest request) {
        Hall hall = hallRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", "id", id));
        applyConfigUpdates(hall, request);
        Hall saved = hallRepository.save(hall);
        return hallMapper.toResponse(saved);
    }

    private void applyConfigUpdates(Hall hall, UpdateHallConfigRequest request) {
        hall.setName(request.getName());
        hall.setDescription(request.getDescription());
        hall.setCapacity(request.getCapacity());
        hall.setHourlyRate(request.getHourlyRate());
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            hall.setCurrency(request.getCurrency());
        }
        hall.setBufferHours(request.getBufferHours());
        hall.setOpenHour(request.getOpenHour());
        hall.setCloseHour(request.getCloseHour());
        if (request.getFeaturesJson() != null) {
            hall.setFeaturesJson(request.getFeaturesJson());
        }
    }
}
