package com.game3alhaq.service;

import com.game3alhaq.dto.request.HallRequests.CreateBlackoutRequest;
import com.game3alhaq.dto.response.HallResponses.BlackoutResponse;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.entity.HallBlackout;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.HallMapper;
import com.game3alhaq.repository.HallBlackoutRepository;
import com.game3alhaq.repository.HallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HallBlackoutServiceTest {

    @Mock
    private HallBlackoutRepository blackoutRepository;

    @Mock
    private HallRepository hallRepository;

    @Mock
    private HallMapper hallMapper;

    @InjectMocks
    private HallBlackoutService blackoutService;

    private Hall hall;
    private HallBlackout blackout;
    private BlackoutResponse blackoutResponse;

    @BeforeEach
    void setUp() {
        hall = Hall.builder().id(1L).name("قاعة المجلس العام").build();
        blackout = HallBlackout.builder()
                .id(5L)
                .hall(hall)
                .blackoutDate(LocalDate.of(2026, 12, 1))
                .startHour(14)
                .endHour(18)
                .reason("حجز خاص")
                .build();
        blackoutResponse = BlackoutResponse.builder()
                .id(5L)
                .hallId(1L)
                .blackoutDate(LocalDate.of(2026, 12, 1))
                .startHour(14)
                .endHour(18)
                .reason("حجز خاص")
                .build();
    }

    @Test
    void testGetBlackoutsByHall() {
        when(blackoutRepository.findAllByHallIdOrderByBlackoutDateAsc(1L)).thenReturn(List.of(blackout));
        when(hallMapper.toBlackoutResponse(blackout)).thenReturn(blackoutResponse);

        List<BlackoutResponse> list = blackoutService.getBlackoutsByHall(1L);

        assertEquals(1, list.size());
        assertEquals(5L, list.get(0).getId());
    }

    @Test
    void testCreateBlackout_Success() {
        CreateBlackoutRequest req = CreateBlackoutRequest.builder()
                .hallId(1L)
                .blackoutDate(LocalDate.of(2026, 12, 1))
                .startHour(14)
                .endHour(18)
                .reason("حجز خاص")
                .build();

        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        when(blackoutRepository.save(any(HallBlackout.class))).thenReturn(blackout);
        when(hallMapper.toBlackoutResponse(blackout)).thenReturn(blackoutResponse);

        BlackoutResponse result = blackoutService.createBlackout(req);

        assertNotNull(result);
        assertEquals(5L, result.getId());
    }

    @Test
    void testCreateBlackout_InvalidHours() {
        CreateBlackoutRequest req = CreateBlackoutRequest.builder()
                .hallId(1L)
                .blackoutDate(LocalDate.of(2026, 12, 1))
                .startHour(18)
                .endHour(14)
                .build();

        assertThrows(IllegalArgumentException.class, () -> blackoutService.createBlackout(req));
    }

    @Test
    void testDeleteBlackout_Success() {
        when(blackoutRepository.existsById(5L)).thenReturn(true);

        blackoutService.deleteBlackout(5L);

        verify(blackoutRepository, times(1)).deleteById(5L);
    }

    @Test
    void testDeleteBlackout_NotFound() {
        when(blackoutRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> blackoutService.deleteBlackout(99L));
    }
}
