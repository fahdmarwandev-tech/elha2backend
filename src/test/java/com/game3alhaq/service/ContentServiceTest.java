package com.game3alhaq.service;

import com.game3alhaq.dto.request.ContentRequests.PrayerTimesUpdateRequest;
import com.game3alhaq.dto.response.ContentResponses.HistoryResponse;
import com.game3alhaq.dto.response.ContentResponses.PrayerTimesResponse;
import com.game3alhaq.entity.PrayersAndHistory;
import com.game3alhaq.repository.PrayersAndHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

    @Mock
    private PrayersAndHistoryRepository prayersAndHistoryRepository;

    @InjectMocks
    private ContentService contentService;

    private PrayersAndHistory existingPrayers;

    @BeforeEach
    void setUp() {
        existingPrayers = PrayersAndHistory.builder()
                .id(1L)
                .fajrTime("05:00")
                .sunriseTime("06:15")
                .dhuhrTime("12:15")
                .asrTime("15:35")
                .maghribTime("18:05")
                .ishaTime("19:25")
                .manualOverride(false)
                .effectiveDate(LocalDate.now())
                .build();
    }

    @Test
    @DisplayName("Should return existing prayer times if effectiveDate is today")
    void testGetPrayerTimesToday() {
        when(prayersAndHistoryRepository.findTopByOrderByEffectiveDateDesc())
                .thenReturn(Optional.of(existingPrayers));

        PrayerTimesResponse response = contentService.getPrayerTimes();

        assertNotNull(response);
        assertEquals("05:00", response.getFajr());
        assertEquals("12:15", response.getDhuhr());
        assertFalse(response.getManualOverride());
        assertNotNull(response.getNextPrayer());
        verify(prayersAndHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return existing prayer times if manualOverride is true, even if outdated")
    void testGetPrayerTimesManualOverride() {
        existingPrayers.setManualOverride(true);
        existingPrayers.setEffectiveDate(LocalDate.now().minusDays(5));
        when(prayersAndHistoryRepository.findTopByOrderByEffectiveDateDesc())
                .thenReturn(Optional.of(existingPrayers));

        PrayerTimesResponse response = contentService.getPrayerTimes();

        assertNotNull(response);
        assertTrue(response.getManualOverride());
        verify(prayersAndHistoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fetch prayer timings when DB is empty")
    void testGetPrayerTimesEmptyDb() {
        when(prayersAndHistoryRepository.findTopByOrderByEffectiveDateDesc())
                .thenReturn(Optional.empty());
        when(prayersAndHistoryRepository.save(any(PrayersAndHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PrayerTimesResponse response = contentService.getPrayerTimes();

        assertNotNull(response);
        assertNotNull(response.getFajr());
        assertNotNull(response.getDhuhr());
        assertNotNull(response.getAsr());
        assertNotNull(response.getMaghrib());
        assertNotNull(response.getIsha());
        assertEquals(LocalDate.now(), response.getEffectiveDate());
        verify(prayersAndHistoryRepository, times(1)).save(any(PrayersAndHistory.class));
    }

    @Test
    @DisplayName("Should update prayer times with manual override enabled")
    void testUpdatePrayerTimes() {
        PrayerTimesUpdateRequest request = PrayerTimesUpdateRequest.builder()
                .fajr("05:10")
                .sunrise("06:20")
                .dhuhr("12:20")
                .asr("15:40")
                .maghrib("18:10")
                .isha("19:30")
                .manualOverride(true)
                .build();

        when(prayersAndHistoryRepository.findTopByOrderByEffectiveDateDesc())
                .thenReturn(Optional.of(existingPrayers));
        when(prayersAndHistoryRepository.save(any(PrayersAndHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PrayerTimesResponse response = contentService.updatePrayerTimes(request);

        assertNotNull(response);
        assertEquals("05:10", response.getFajr());
        assertEquals("18:10", response.getMaghrib());
        assertTrue(response.getManualOverride());
        verify(prayersAndHistoryRepository, times(1)).save(any(PrayersAndHistory.class));
    }

    @Test
    @DisplayName("Should return complete mosque history and facilities overview")
    void testGetHistory() {
        HistoryResponse response = contentService.getHistory();

        assertNotNull(response);
        assertEquals("جامع الحق ودار المناسبات", response.getMosqueName());
        assertEquals(2011, response.getInaugurationYear());
        assertEquals(1800, response.getWorshippersCapacity());
        assertEquals(700, response.getMajlisCapacity());
        assertFalse(response.getFacilities().isEmpty());
        assertFalse(response.getWaqfProjects().isEmpty());
    }
}
