package com.game3alhaq.service;

import com.game3alhaq.dto.request.BookingRequests.AdminCreateBookingRequest;
import com.game3alhaq.dto.request.BookingRequests.CreateBookingRequest;
import com.game3alhaq.dto.request.BookingRequests.UpdateBookingStatusRequest;
import com.game3alhaq.dto.response.BookingResponses.BookingInitiationResponse;
import com.game3alhaq.dto.response.BookingResponses.BookingResponse;
import com.game3alhaq.entity.Booking;
import com.game3alhaq.entity.Hall;
import com.game3alhaq.entity.HallBlackout;
import com.game3alhaq.entity.Payment;
import com.game3alhaq.exception.BookingConflictException;
import com.game3alhaq.mapper.BookingMapper;
import com.game3alhaq.repository.BookingRepository;
import com.game3alhaq.repository.HallBlackoutRepository;
import com.game3alhaq.repository.HallRepository;
import com.game3alhaq.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private HallRepository hallRepository;
    @Mock
    private HallBlackoutRepository blackoutRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentService paymentService;
    @Mock
    private MailService mailService;
    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private Hall hall;
    private CreateBookingRequest createRequest;

    @BeforeEach
    void setUp() {
        hall = Hall.builder()
                .id(1L).name("قاعة المجلس العام").slug("al-majlis")
                .capacity(700).hourlyRate(BigDecimal.valueOf(10.0)).currency("OMR")
                .bufferHours(1).openHour(13).closeHour(24).isActive(true).build();

        createRequest = CreateBookingRequest.builder()
                .hallId(1L).fullName("أحمد البلوشي").nationalId("12345678")
                .phone("96891234567").secondaryPhone("96897654321").email("ahmed@example.com")
                .bookingType("KATB_KETAB").eventDate(LocalDate.now().plusDays(5))
                .startHour(16).endHour(19).notes("مناسبة عائلية").build();
    }

    @Test
    @DisplayName("Successfully initiate booking with PayPal checkout")
    void createBooking_Success() {
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        when(blackoutRepository.findAllByHallIdAndBlackoutDate(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingRepository.findConflictingBookingsWithBuffer(anyLong(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());
        when(bookingRepository.findByCode(anyString())).thenReturn(Optional.empty());

        Booking savedBooking = Booking.builder()
                .id(100L).code("DAR-A1B2C3").hall(hall).totalAmount(BigDecimal.valueOf(30.0))
                .status("PENDING_PAYMENT").build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(savedBooking);

        Payment payment = Payment.builder()
                .id(200L).paypalOrderId("ORDER-123").approvalUrl("https://paypal.com/approve").build();
        when(paymentService.createPayPalOrder(savedBooking)).thenReturn(payment);

        BookingResponse response = BookingResponse.builder().id(100L).code("DAR-A1B2C3").build();
        when(bookingMapper.toResponse(savedBooking)).thenReturn(response);

        BookingInitiationResponse result = bookingService.createBooking(createRequest);

        assertNotNull(result);
        assertEquals("ORDER-123", result.getPaypalOrderId());
        assertEquals("https://paypal.com/approve", result.getApprovalUrl());
        verify(paymentService, times(1)).createPayPalOrder(savedBooking);
    }

    @Test
    @DisplayName("Fail booking creation when conflicting with existing reservation turnaround buffer")
    void createBooking_BufferConflict_ThrowsException() {
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        when(blackoutRepository.findAllByHallIdAndBlackoutDate(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());

        Booking existing = Booking.builder().id(50L).startHour(14).endHour(16).build();
        when(bookingRepository.findConflictingBookingsWithBuffer(anyLong(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(existing));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(createRequest));
        verify(bookingRepository, never()).save(any());
        verify(paymentService, never()).createPayPalOrder(any());
    }

    @Test
    @DisplayName("Fail booking when date falls on a full-day blackout")
    void createBooking_FullDayBlackout_ThrowsException() {
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        HallBlackout blackout = HallBlackout.builder()
                .id(1L).blackoutDate(createRequest.getEventDate()).reason("صيانة دورية للمكيفات").build();
        when(blackoutRepository.findAllByHallIdAndBlackoutDate(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(blackout));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(createRequest));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fail booking when hours are outside hall operating hours")
    void createBooking_OutsideOperatingHours_ThrowsException() {
        createRequest.setStartHour(11);
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(createRequest));
    }

    @Test
    @DisplayName("Fail booking when start hour is greater than or equal to end hour")
    void createBooking_InvalidHourRange_ThrowsException() {
        createRequest.setStartHour(18);
        createRequest.setEndHour(16);
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(createRequest));
    }

    @Test
    @DisplayName("Admin manual booking with skipPayment confirms immediately and sends email")
    void adminCreateBooking_SkipPayment_ConfirmedImmediately() {
        AdminCreateBookingRequest adminReq = AdminCreateBookingRequest.builder()
                .hallId(1L).fullName("سالم الحارثي").nationalId("87654321")
                .phone("96898765432").email("salem@example.com").bookingType("AZA")
                .eventDate(LocalDate.now().plusDays(2)).startHour(14).endHour(18)
                .skipPayment(true).build();

        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        when(blackoutRepository.findAllByHallIdAndBlackoutDate(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingRepository.findConflictingBookingsWithBuffer(anyLong(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        Booking confirmedBooking = Booking.builder()
                .id(101L).code("DAR-X9Y8Z7").status("CONFIRMED").build();
        when(bookingRepository.save(any(Booking.class))).thenReturn(confirmedBooking);
        when(bookingMapper.toResponse(confirmedBooking))
                .thenReturn(BookingResponse.builder().id(101L).status("CONFIRMED").build());

        BookingResponse res = bookingService.adminCreateBooking(adminReq);

        assertEquals("CONFIRMED", res.getStatus());
        verify(mailService, times(1)).sendBookingConfirmation(confirmedBooking);
        verify(paymentService, never()).createPayPalOrder(any());
    }

    @Test
    @DisplayName("Admin updates booking status successfully")
    void updateBookingStatus_Success() {
        Booking booking = Booking.builder().id(10L).status("PENDING_PAYMENT").build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(booking)).thenReturn(booking);
        when(bookingMapper.toResponse(booking))
                .thenReturn(BookingResponse.builder().id(10L).status("CONFIRMED").build());

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest("CONFIRMED");
        BookingResponse res = bookingService.updateBookingStatus(10L, req);

        assertEquals("CONFIRMED", res.getStatus());
    }

    @Test
    @DisplayName("Admin updating booking status with invalid value throws exception")
    void updateBookingStatus_InvalidStatus_ThrowsException() {
        Booking booking = Booking.builder().id(10L).status("PENDING_PAYMENT").build();
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));

        UpdateBookingStatusRequest req = new UpdateBookingStatusRequest("UNKNOWN_STATUS");
        assertThrows(IllegalArgumentException.class, () -> bookingService.updateBookingStatus(10L, req));
    }

    @Test
    @DisplayName("Cleanup expired bookings executes repository batch update")
    void cleanupExpiredBookings_ExecutesModifyingQuery() {
        when(bookingRepository.markExpiredBookings(any(OffsetDateTime.class))).thenReturn(3);

        bookingService.cleanupExpiredBookings();

        verify(bookingRepository, times(1)).markExpiredBookings(any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("Fail 3aza booking when startHour is before Dhuhr (12:00)")
    void createBooking_AzaBeforeDhuhr_ThrowsException() {
        hall.setOpenHour(0);
        hall.setCloseHour(24);
        createRequest.setBookingType("3aza");
        createRequest.setStartHour(9);
        createRequest.setEndHour(15);
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(createRequest));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fail 3aza booking when endHour exceeds Maghrib time (18:00)")
    void createBooking_AzaAfterMaghrib_ThrowsException() {
        hall.setOpenHour(0);
        hall.setCloseHour(24);
        createRequest.setBookingType("3aza");
        createRequest.setStartHour(17);
        createRequest.setEndHour(19);
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(createRequest));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("3aza booking from Dhuhr to Maghrib spans 3 consecutive days and validates all 3 days")
    void createBooking_AzaSpansThreeDays_Success() {
        hall.setOpenHour(0);
        hall.setCloseHour(24);
        LocalDate start = LocalDate.of(2026, 11, 10);
        createRequest.setBookingType("3aza");
        createRequest.setEventDate(start);
        createRequest.setStartHour(12);
        createRequest.setEndHour(18); // 6 hours/day * 3 days = 18 hours * 10 OMR = 180 OMR

        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        when(blackoutRepository.findAllByHallIdAndBlackoutDate(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingRepository.findConflictingBookingsWithBuffer(anyLong(), any(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());
        when(bookingRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = Payment.builder()
                .id(201L).paypalOrderId("ORDER-AZA").approvalUrl("https://paypal.com/aza").build();
        when(paymentService.createPayPalOrder(any(Booking.class))).thenReturn(payment);
        when(bookingMapper.toResponse(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            return BookingResponse.builder()
                    .eventDate(b.getEventDate())
                    .endDate(b.getEndDate())
                    .daysCount(b.getDaysCount())
                    .totalAmount(b.getTotalAmount())
                    .build();
        });

        BookingInitiationResponse res = bookingService.createBooking(createRequest);

        assertEquals(start, res.getBooking().getEventDate());
        assertEquals(start.plusDays(2), res.getBooking().getEndDate());
        assertEquals(3, res.getBooking().getDaysCount());
        assertEquals(List.of(start, start.plusDays(1), start.plusDays(2)), res.getBooking().getReservedDates());
        assertEquals("عزاء (3 أيام)", res.getBooking().getEventLabel());
        assertEquals("من 12:00 ظهراً (الظهر) إلى 06:00 مساءً (المغرب)", res.getBooking().getFormattedTimeRange());
        assertEquals(0, BigDecimal.valueOf(180.0).compareTo(res.getBooking().getTotalAmount()));
        verify(bookingRepository, times(3)).findConflictingBookingsWithBuffer(
                eq(1L), any(LocalDate.class), eq(12), eq(18), eq(1), any());
    }

    @Test
    @DisplayName("Fail 3aza booking when hall is reserved for katbketab between Dhuhr and Maghrib on any of the 3 days")
    void createBooking_AzaConflictsWithKatbKetabOnSharedHall_ThrowsException() {
        hall.setOpenHour(0);
        hall.setCloseHour(24);
        LocalDate day1 = LocalDate.of(2026, 11, 10);
        LocalDate day2 = LocalDate.of(2026, 11, 11);
        createRequest.setBookingType("3aza");
        createRequest.setEventDate(day1);
        createRequest.setStartHour(12);
        createRequest.setEndHour(18);

        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));
        when(blackoutRepository.findAllByHallIdAndBlackoutDate(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());
        when(bookingRepository.findConflictingBookingsWithBuffer(eq(1L), eq(day1), eq(12), eq(18), eq(1), any()))
                .thenReturn(List.of());

        Booking existingKatbKetab = Booking.builder()
                .id(77L).bookingType("katbketab").eventDate(day2).startHour(14).endHour(16).build();
        when(bookingRepository.findConflictingBookingsWithBuffer(eq(1L), eq(day2), eq(12), eq(18), eq(1), any()))
                .thenReturn(List.of(existingKatbKetab));

        assertThrows(BookingConflictException.class, () -> bookingService.createBooking(createRequest));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Fail booking when eventDate is in the past")
    void createBooking_PastDate_ThrowsException() {
        createRequest.setEventDate(LocalDate.now().minusDays(1));
        when(hallRepository.findById(1L)).thenReturn(Optional.of(hall));

        assertThrows(IllegalArgumentException.class, () -> bookingService.createBooking(createRequest));
        verify(bookingRepository, never()).save(any());
    }
}
