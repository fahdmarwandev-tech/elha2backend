package com.game3alhaq.service;

import com.game3alhaq.dto.request.HallRequests.UpdateHallConfigRequest;
import com.game3alhaq.dto.response.HallResponses.HallResponse;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.HallMapper;
import com.game3alhaq.repository.HallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HallServiceTest {

    @Mock
    private HallRepository hallRepository;

    @Mock
    private HallMapper hallMapper;

    @InjectMocks
    private HallService hallService;

    private Hall hall;
    private HallResponse hallResponse;

    @BeforeEach
    void setUp() {
        hall = Hall.builder()
                .id(1L)
                .name("قاعة المجلس العام")
                .slug("al-majlis-al-aam")
                .capacity(700)
                .hourlyRate(BigDecimal.valueOf(10.0))
                .currency("OMR")
                .bufferHours(1)
                .openHour(13)
                .closeHour(24)
                .isActive(true)
                .build();

        hallResponse = HallResponse.builder()
                .id(1L)
                .name("قاعة المجلس العام")
                .slug("al-majlis-al-aam")
                .capacity(700)
                .hourlyRate(BigDecimal.valueOf(10.0))
                .bufferHours(1)
                .openHour(13)
                .closeHour(24)
                .build();
    }

    @Test
    void testGetActiveHalls() {
        when(hallRepository.findAllActiveWithMedia()).thenReturn(List.of(hall));
        when(hallMapper.toResponse(hall)).thenReturn(hallResponse);

        List<HallResponse> result = hallService.getActiveHalls();

        assertEquals(1, result.size());
        assertEquals("al-majlis-al-aam", result.get(0).getSlug());
    }

    @Test
    void testGetHallById_Found() {
        when(hallRepository.findByIdWithMedia(1L)).thenReturn(Optional.of(hall));
        when(hallMapper.toResponse(hall)).thenReturn(hallResponse);

        HallResponse result = hallService.getHallById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void testGetHallById_NotFound() {
        when(hallRepository.findByIdWithMedia(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> hallService.getHallById(99L));
    }

    @Test
    void testUpdateHallConfig() {
        UpdateHallConfigRequest req = UpdateHallConfigRequest.builder()
                .name("قاعة المجلس المحدثة")
                .capacity(800)
                .hourlyRate(BigDecimal.valueOf(15.0))
                .bufferHours(2)
                .openHour(12)
                .closeHour(24)
                .build();

        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        when(hallRepository.save(any(Hall.class))).thenReturn(hall);
        when(hallMapper.toResponse(hall)).thenReturn(hallResponse);

        HallResponse result = hallService.updateHallConfig(1L, req);

        assertNotNull(result);
        assertEquals("قاعة المجلس المحدثة", hall.getName());
        assertEquals(800, hall.getCapacity());
        assertEquals(2, hall.getBufferHours());
        assertEquals(12, hall.getOpenHour());
    }
}
