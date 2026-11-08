package com.game3alhaq.service;

import com.game3alhaq.dto.response.HallResponses.DayAvailabilityResponse;
import com.game3alhaq.dto.response.HallResponses.HourlyAvailabilityResponse;
import com.game3alhaq.dto.response.HallResponses.HourSlotStatus;
import com.game3alhaq.entity.Booking;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.entity.HallBlackout;
import com.game3alhaq.repository.BookingRepository;
import com.game3alhaq.repository.HallBlackoutRepository;
import com.game3alhaq.repository.HallRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private HallRepository hallRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private HallBlackoutRepository blackoutRepository;

    @InjectMocks
    private TimeSlotService timeSlotService;

    private Hall testHall;

    @BeforeEach
    void setUp() {
        testHall = Hall.builder()
                .id(1L)
                .name("قاعة المجلس العام")
                .slug("al-majlis-al-aam")
                .bufferHours(1)
                .openHour(13)
                .closeHour(24)
                .hourlyRate(BigDecimal.valueOf(10.0))
                .build();
    }

    @Test
    void testAvailabilityWithBookingAndBuffer() {
        LocalDate eventDate = LocalDate.of(2026, 11, 15);
        Booking booking = Booking.builder()
                .id(10L)
                .code("DAR-TEST01")
                .hall(testHall)
                .eventDate(eventDate)
                .startHour(16)
                .endHour(18)
                .status("CONFIRMED")
                .build();

        when(hallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(bookingRepository.findActiveBookingsInDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(booking));
        when(blackoutRepository.findAllByHallIdAndBlackoutDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        HourlyAvailabilityResponse response = timeSlotService.getMonthlyAvailability(1L, 2026, 11);

        assertNotNull(response);
        assertEquals(30, response.getDays().size());

        DayAvailabilityResponse day15 = response.getDays().stream()
                .filter(d -> d.getDayNumber() == 15)
                .findFirst()
                .orElseThrow();

        assertEquals("PARTIALLY_BOOKED", day15.getStatus());

        assertSlotStatus(day15, 14, "AVAILABLE");
        assertSlotStatus(day15, 15, "BUFFER");
        assertSlotStatus(day15, 16, "BOOKED");
        assertSlotStatus(day15, 17, "BOOKED");
        assertSlotStatus(day15, 18, "BUFFER");
        assertSlotStatus(day15, 19, "AVAILABLE");
    }

    @Test
    void testFullDayBlackout() {
        LocalDate blackoutDate = LocalDate.of(2026, 11, 20);
        HallBlackout blackout = HallBlackout.builder()
                .id(1L)
                .hall(testHall)
                .blackoutDate(blackoutDate)
                .startHour(null)
                .endHour(null)
                .reason("صيانة دورية")
                .build();

        when(hallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(bookingRepository.findActiveBookingsInDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());
        when(blackoutRepository.findAllByHallIdAndBlackoutDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(blackout));

        HourlyAvailabilityResponse response = timeSlotService.getMonthlyAvailability(1L, 2026, 11);

        DayAvailabilityResponse day20 = response.getDays().stream()
                .filter(d -> d.getDayNumber() == 20)
                .findFirst()
                .orElseThrow();

        assertEquals("FULLY_BOOKED", day20.getStatus());
        for (HourSlotStatus slot : day20.getHours()) {
            assertEquals("BLACKOUT", slot.getStatus());
        }
    }

    @Test
    void testThreeDayAzaBookingMarksAllThreeDays() {
        testHall.setOpenHour(0);
        testHall.setCloseHour(24);
        LocalDate startDate = LocalDate.of(2026, 11, 10);
        Booking azaBooking = Booking.builder()
                .id(25L)
                .code("DAR-AZA003")
                .fullName("سالم الحارثي")
                .hall(testHall)
                .bookingType("3aza")
                .eventDate(startDate)
                .endDate(startDate.plusDays(2))
                .daysCount(3)
                .startHour(12)
                .endHour(18)
                .status("CONFIRMED")
                .build();

        when(hallRepository.findById(1L)).thenReturn(Optional.of(testHall));
        when(bookingRepository.findActiveBookingsInDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(azaBooking));
        when(blackoutRepository.findAllByHallIdAndBlackoutDateBetween(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        HourlyAvailabilityResponse response = timeSlotService.getMonthlyAvailability(1L, 2026, 11);

        assertEquals(12, response.getDhuhrHour());
        assertEquals(18, response.getMaghribHour());
        List<String> expectedThreeDays = List.of("2026-11-10", "2026-11-11", "2026-11-12");

        int expectedIdx = 1;
        for (int dayNum : List.of(10, 11, 12)) {
            DayAvailabilityResponse d = response.getDays().stream()
                    .filter(day -> day.getDayNumber() == dayNum)
                    .findFirst()
                    .orElseThrow();
            assertEquals("PARTIALLY_BOOKED", d.getStatus());
            assertSlotStatus(d, 11, "BUFFER");
            for (int h = 12; h < 18; h++) {
                final int currentHour = h;
                assertSlotStatus(d, currentHour, "BOOKED");
                HourSlotStatus slot = d.getHours().stream().filter(s -> s.getHour() == currentHour).findFirst().orElseThrow();
                assertEquals("3aza", slot.getBookingType());
                assertEquals("عزاء", slot.getEventLabel());
            }
            assertSlotStatus(d, 18, "BUFFER");

            assertNotNull(d.getReservations());
            assertEquals(1, d.getReservations().size());
            var summary = d.getReservations().get(0);
            assertEquals("3aza", summary.getBookingType());
            assertEquals("عزاء (3 أيام)", summary.getEventLabel());
            assertEquals(3, summary.getDaysCount());
            assertEquals(expectedIdx++, summary.getDayIndex());
            assertEquals(expectedThreeDays, summary.getReservedDates());
            assertEquals(12, summary.getStartHour());
            assertEquals(18, summary.getEndHour());
            assertEquals("من 12:00 ظهراً (الظهر) إلى 06:00 مساءً (المغرب)", summary.getFormattedTimeRange());
        }

        // Verify days 6 and 7 (before today Nov 8, 2026) are marked FULLY_BOOKED / BLACKOUT
        for (int pastDayNum : List.of(6, 7)) {
            DayAvailabilityResponse pastDay = response.getDays().stream()
                    .filter(day -> day.getDayNumber() == pastDayNum)
                    .findFirst()
                    .orElseThrow();
            assertEquals("FULLY_BOOKED", pastDay.getStatus());
            for (HourSlotStatus slot : pastDay.getHours()) {
                assertEquals("BLACKOUT", slot.getStatus());
            }
        }
    }

    private void assertSlotStatus(DayAvailabilityResponse day, int hour, String expectedStatus) {
        HourSlotStatus slot = day.getHours().stream()
                .filter(s -> s.getHour() == hour)
                .findFirst()
                .orElseThrow();
        assertEquals(expectedStatus, slot.getStatus(), "Mismatch for hour " + hour);
    }
}
