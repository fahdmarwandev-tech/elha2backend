package com.game3alhaq.service;

import com.game3alhaq.dto.response.HallResponses.*;
import com.game3alhaq.entity.Booking;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.entity.HallBlackout;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.repository.BookingRepository;
import com.game3alhaq.repository.HallBlackoutRepository;
import com.game3alhaq.repository.HallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    public static final int DEFAULT_DHUHR_HOUR = 12;
    public static final String DEFAULT_DHUHR_TIME = "12:15";
    public static final int DEFAULT_MAGHRIB_HOUR = 18;
    public static final String DEFAULT_MAGHRIB_TIME = "18:05";

    private final HallRepository hallRepository;
    private final BookingRepository bookingRepository;
    private final HallBlackoutRepository blackoutRepository;

    @Transactional(readOnly = true)
    public HourlyAvailabilityResponse getMonthlyAvailability(Long hallId, int year, int month) {
        validateMonth(month);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", "id", hallId));
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        List<Booking> bookings = bookingRepository.findActiveBookingsInDateRange(
                hallId, startDate, endDate, OffsetDateTime.now());
        List<HallBlackout> blackouts = blackoutRepository.findAllByHallIdAndBlackoutDateBetween(
                hallId, startDate, endDate);

        List<DayAvailabilityResponse> days = buildDaysAvailability(
                startDate, endDate, hall, bookings, blackouts);

        return buildAvailabilityResponse(hall, year, month, days);
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12");
        }
    }

    private HourlyAvailabilityResponse buildAvailabilityResponse(
            Hall hall, int year, int month, List<DayAvailabilityResponse> days) {
        return HourlyAvailabilityResponse.builder()
                .hallId(hall.getId())
                .hallName(hall.getName())
                .year(year)
                .month(month)
                .bufferHours(hall.getBufferHours())
                .openHour(hall.getOpenHour() != null ? hall.getOpenHour() : 0)
                .closeHour(hall.getCloseHour() != null ? hall.getCloseHour() : 24)
                .dhuhrHour(DEFAULT_DHUHR_HOUR)
                .dhuhrTime(DEFAULT_DHUHR_TIME)
                .maghribHour(DEFAULT_MAGHRIB_HOUR)
                .maghribTime(DEFAULT_MAGHRIB_TIME)
                .days(days)
                .build();
    }

    private List<DayAvailabilityResponse> buildDaysAvailability(
            LocalDate startDate, LocalDate endDate, Hall hall,
            List<Booking> bookings, List<HallBlackout> blackouts) {
        Map<LocalDate, List<Booking>> bookingsByDate = groupBookingsByDate(bookings, startDate, endDate);
        Map<LocalDate, List<HallBlackout>> blackoutsByDate = blackouts.stream()
                .collect(Collectors.groupingBy(HallBlackout::getBlackoutDate));

        List<DayAvailabilityResponse> dayList = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Booking> dayBookings = bookingsByDate.getOrDefault(date, List.of());
            List<HallBlackout> dayBlackouts = blackoutsByDate.getOrDefault(date, List.of());
            dayList.add(buildSingleDayAvailability(date, hall, dayBookings, dayBlackouts));
        }
        return dayList;
    }

    private Map<LocalDate, List<Booking>> groupBookingsByDate(
            List<Booking> bookings, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<Booking>> map = new HashMap<>();
        for (Booking b : bookings) {
            if (b.getEventDate() == null) {
                continue;
            }
            LocalDate from = b.getEventDate().isBefore(startDate) ? startDate : b.getEventDate();
            LocalDate rawEnd = resolveBookingEndDate(b);
            LocalDate to = rawEnd.isAfter(endDate) ? endDate : rawEnd;
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                map.computeIfAbsent(d, k -> new ArrayList<>()).add(b);
            }
        }
        return map;
    }

    private boolean isAzaBooking(Booking b) {
        if (b == null || b.getBookingType() == null) return false;
        String bt = b.getBookingType().trim().toLowerCase();
        return "3aza".equals(bt) || "aza".equals(bt) || bt.contains("عزاء");
    }

    private LocalDate resolveBookingEndDate(Booking b) {
        LocalDate rawEnd = b.getEndDate();
        if (rawEnd == null || rawEnd.isEqual(b.getEventDate()) || rawEnd.isBefore(b.getEventDate())) {
            if (isAzaBooking(b)) {
                return b.getEventDate().plusDays(2);
            }
            return b.getEventDate();
        }
        return rawEnd;
    }

    private DayAvailabilityResponse buildSingleDayAvailability(
            LocalDate date, Hall hall,
            List<Booking> dayBookings, List<HallBlackout> dayBlackouts) {
        boolean isPastDate = date.isBefore(LocalDate.now());
        List<HourSlotStatus> hourSlots = buildHourlySlots(hall, dayBookings, dayBlackouts, isPastDate);
        List<DayReservationSummary> reservations = buildDayReservationSummaries(date, dayBookings);
        int availableCount = (int) hourSlots.stream()
                .filter(slot -> "AVAILABLE".equals(slot.getStatus()))
                .count();
        String dayStatus = isPastDate ? "FULLY_BOOKED" : determineDayStatus(availableCount, hourSlots.size());

        return DayAvailabilityResponse.builder()
                .date(date)
                .dayNumber(date.getDayOfMonth())
                .status(dayStatus)
                .hours(hourSlots)
                .reservations(reservations)
                .build();
    }

    private List<DayReservationSummary> buildDayReservationSummaries(LocalDate date, List<Booking> dayBookings) {
        List<DayReservationSummary> list = new ArrayList<>();
        for (Booking b : dayBookings) {
            boolean isAza = isAzaBooking(b);
            LocalDate start = b.getEventDate();
            LocalDate end = resolveBookingEndDate(b);
            List<String> reservedDates = new ArrayList<>();
            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                reservedDates.add(d.toString());
            }
            int daysCount = isAza ? 3 : Math.max(1, reservedDates.size());
            int dayIndex = Math.max(1, (int) (date.toEpochDay() - start.toEpochDay() + 1));
            int sh = b.getStartHour() != null ? b.getStartHour() : DEFAULT_DHUHR_HOUR;
            int eh = b.getEndHour() != null ? b.getEndHour() : DEFAULT_MAGHRIB_HOUR;
            String normalizedType = isAza ? "3aza" : "katbketab";
            String eventLabel = isAza ? "عزاء (3 أيام)" : "عقد قران";
            String formattedTimeRange = isAza && sh == DEFAULT_DHUHR_HOUR && eh == DEFAULT_MAGHRIB_HOUR
                    ? "من 12:00 ظهراً (الظهر) إلى 06:00 مساءً (المغرب)"
                    : "من " + formatHour(sh) + " إلى " + formatHour(eh);

            list.add(DayReservationSummary.builder()
                    .bookingId(b.getId())
                    .code(b.getCode())
                    .fullName(b.getFullName())
                    .bookingType(normalizedType)
                    .eventLabel(eventLabel)
                    .eventDate(start)
                    .endDate(end)
                    .reservedDates(reservedDates)
                    .daysCount(daysCount)
                    .dayIndex(dayIndex)
                    .startHour(sh)
                    .endHour(eh)
                    .formattedTimeRange(formattedTimeRange)
                    .status(b.getStatus())
                    .build());
        }
        return list;
    }

    private List<HourSlotStatus> buildHourlySlots(
            Hall hall, List<Booking> dayBookings, List<HallBlackout> dayBlackouts, boolean isPastDate) {
        int open = hall.getOpenHour() != null ? hall.getOpenHour() : 0;
        int close = hall.getCloseHour() != null ? hall.getCloseHour() : 24;
        int buffer = hall.getBufferHours() != null ? hall.getBufferHours() : 1;

        List<HourSlotStatus> slots = new ArrayList<>();
        for (int h = open; h < close; h++) {
            String status = isPastDate ? "BLACKOUT" : determineHourStatus(h, buffer, dayBookings, dayBlackouts);
            Booking matched = isPastDate ? null : findMatchedBookingForHour(h, buffer, dayBookings);
            String bookingType = null;
            String eventLabel = null;
            String bookingCode = null;
            if (matched != null) {
                boolean isAza = isAzaBooking(matched);
                bookingType = isAza ? "3aza" : "katbketab";
                eventLabel = isAza ? "عزاء" : "عقد قران";
                bookingCode = matched.getCode();
            }
            slots.add(HourSlotStatus.builder()
                    .hour(h)
                    .formattedHour(formatHour(h))
                    .status(status)
                    .bookingType(bookingType)
                    .eventLabel(eventLabel)
                    .bookingCode(bookingCode)
                    .build());
        }
        return slots;
    }

    private Booking findMatchedBookingForHour(int hour, int buffer, List<Booking> bookings) {
        for (Booking b : bookings) {
            if (b.getStartHour() != null && b.getEndHour() != null
                    && hour >= b.getStartHour() && hour < b.getEndHour()) {
                return b;
            }
        }
        if (buffer > 0) {
            for (Booking b : bookings) {
                if (b.getStartHour() == null || b.getEndHour() == null) continue;
                boolean before = hour >= (b.getStartHour() - buffer) && hour < b.getStartHour();
                boolean after = hour >= b.getEndHour() && hour < (b.getEndHour() + buffer);
                if (before || after) {
                    return b;
                }
            }
        }
        return null;
    }

    private String determineHourStatus(
            int hour, int buffer,
            List<Booking> bookings, List<HallBlackout> blackouts) {
        if (isHourBooked(hour, bookings)) {
            return "BOOKED";
        }
        if (isHourBuffer(hour, buffer, bookings)) {
            return "BUFFER";
        }
        if (isHourBlackout(hour, blackouts)) {
            return "BLACKOUT";
        }
        return "AVAILABLE";
    }

    private boolean isHourBooked(int hour, List<Booking> bookings) {
        return bookings.stream().anyMatch(b ->
                b.getStartHour() != null && b.getEndHour() != null
                && hour >= b.getStartHour() && hour < b.getEndHour()
        );
    }

    private boolean isHourBuffer(int hour, int buffer, List<Booking> bookings) {
        if (buffer <= 0) return false;
        return bookings.stream().anyMatch(b -> {
            if (b.getStartHour() == null || b.getEndHour() == null) return false;
            boolean before = hour >= (b.getStartHour() - buffer) && hour < b.getStartHour();
            boolean after = hour >= b.getEndHour() && hour < (b.getEndHour() + buffer);
            return before || after;
        });
    }

    private boolean isHourBlackout(int hour, List<HallBlackout> blackouts) {
        return blackouts.stream().anyMatch(b -> {
            if (b.getStartHour() == null || b.getEndHour() == null) {
                return true;
            }
            return hour >= b.getStartHour() && hour < b.getEndHour();
        });
    }

    private String determineDayStatus(int availableHours, int totalHours) {
        if (availableHours == 0) {
            return "FULLY_BOOKED";
        }
        if (availableHours == totalHours) {
            return "AVAILABLE";
        }
        return "PARTIALLY_BOOKED";
    }

    private String formatHour(int hour) {
        int h12 = hour % 12;
        if (h12 == 0) {
            h12 = 12;
        }
        String period = (hour >= 12 && hour < 24) ? "م" : "ص";
        return String.format("%02d:00 %s", h12, period);
    }
}
