package com.game3alhaq.service;

import com.game3alhaq.dto.request.BookingRequests.AdminCreateBookingRequest;
import com.game3alhaq.dto.request.BookingRequests.CreateBookingRequest;
import com.game3alhaq.dto.request.BookingRequests.UpdateBookingStatusRequest;
import com.game3alhaq.dto.response.BookingResponses.BookingInitiationResponse;
import com.game3alhaq.dto.response.BookingResponses.BookingResponse;
import com.game3alhaq.entity.*;
import com.game3alhaq.exception.BookingConflictException;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.BookingMapper;
import com.game3alhaq.repository.BookingRepository;
import com.game3alhaq.repository.HallBlackoutRepository;
import com.game3alhaq.repository.HallRepository;
import com.game3alhaq.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    public static final int MAGHRIB_CUTOFF_HOUR = 18;
    public static final int AZA_DAYS_COUNT = 3;

    private final BookingRepository bookingRepository;
    private final HallRepository hallRepository;
    private final HallBlackoutRepository blackoutRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final MailService mailService;
    private final BookingMapper bookingMapper;

    @Transactional
    public BookingInitiationResponse createBooking(CreateBookingRequest req) {
        Hall hall = getActiveHall(req.getHallId());
        validateTiming(hall, req.getBookingType(), req.getStartHour(), req.getEndHour());
        int daysCount = resolveDaysCount(req.getBookingType());
        LocalDate endDate = req.getEventDate().plusDays(daysCount - 1L);
        validateDateRangeAvailability(hall, req.getEventDate(), endDate, req.getStartHour(), req.getEndHour());
        User user = resolveCurrentUser();
        Booking booking = initBookingFromRequest(hall, user, req, daysCount, endDate);
        booking.setStatus("PENDING_PAYMENT");
        booking.setPaymentExpiresAt(OffsetDateTime.now().plusMinutes(15));
        Booking saved = bookingRepository.save(booking);
        Payment payment = paymentService.createPayPalOrder(saved);
        return new BookingInitiationResponse(
                bookingMapper.toResponse(saved), payment.getPaypalOrderId(), payment.getApprovalUrl());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse adminCreateBooking(AdminCreateBookingRequest req) {
        Hall hall = getActiveHall(req.getHallId());
        validateTiming(hall, req.getBookingType(), req.getStartHour(), req.getEndHour());
        int daysCount = resolveDaysCount(req.getBookingType());
        LocalDate endDate = req.getEventDate().plusDays(daysCount - 1L);
        validateDateRangeAvailability(hall, req.getEventDate(), endDate, req.getStartHour(), req.getEndHour());
        Booking booking = initBookingFromAdmin(hall, req, daysCount, endDate);
        boolean skip = Boolean.TRUE.equals(req.getSkipPayment());
        booking.setStatus(skip ? "CONFIRMED" : "PENDING_PAYMENT");
        booking.setPaymentExpiresAt(skip ? null : OffsetDateTime.now().plusMinutes(15));
        Booking saved = bookingRepository.save(booking);
        handleAdminPayment(saved, skip);
        return bookingMapper.toResponse(saved);
    }

    private void handleAdminPayment(Booking saved, boolean skip) {
        if (skip) {
            mailService.sendBookingConfirmation(saved);
        } else {
            paymentService.createPayPalOrder(saved);
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByCode(String code) {
        Booking booking = bookingRepository.findByCodeWithDetails(code)
                .or(() -> bookingRepository.findByCode(code))
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "code", code));
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        return bookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String userPhoneOrEmail) {
        User user = userRepository.findByPhone(userPhoneOrEmail)
                .or(() -> userRepository.findByEmail(userPhoneOrEmail)).orElse(null);
        Long userId = user != null ? user.getId() : null;
        List<Booking> bookings = bookingRepository.findUserBookings(userId, userPhoneOrEmail);
        return bookingMapper.toResponseList(bookings);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public List<BookingResponse> getAllBookingsAdmin(
            String status, String bookingType, LocalDate fromDate, LocalDate toDate) {
        List<Booking> bookings = bookingRepository.findAdminBookings(status, bookingType, fromDate, toDate);
        return bookingMapper.toResponseList(bookings);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BookingResponse updateBookingStatus(Long id, UpdateBookingStatusRequest req) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        String newStatus = req.getStatus().toUpperCase();
        validateStatusTransition(newStatus);
        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);
        return bookingMapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", id));
        bookingRepository.delete(booking);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredBookings() {
        int count = bookingRepository.markExpiredBookings(OffsetDateTime.now());
        if (count > 0) {
            log.info("Expired bookings cleanup: marked {} bookings as EXPIRED", count);
        }
    }

    private Hall getActiveHall(Long hallId) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", "id", hallId));
        if (Boolean.FALSE.equals(hall.getIsActive())) {
            throw new IllegalArgumentException("Hall is currently inactive for reservations");
        }
        return hall;
    }

    private void validateTiming(Hall hall, String bookingType, int startHour, int endHour) {
        if (startHour >= endHour) {
            throw new IllegalArgumentException("Start hour must be strictly before end hour");
        }
        int open = hall.getOpenHour() != null ? hall.getOpenHour() : 0;
        int close = hall.getCloseHour() != null ? hall.getCloseHour() : 24;
        if (startHour < open || endHour > close) {
            throw new IllegalArgumentException(String.format(
                    "Booking hours (%d-%d) outside hall operating hours (%d-%d)",
                    startHour, endHour, open, close));
        }
        if (isAzaBooking(bookingType) && endHour > MAGHRIB_CUTOFF_HOUR) {
            throw new IllegalArgumentException(
                    "لا يمكن حجز مجلس عزاء بعد وقت أذان المغرب (" + MAGHRIB_CUTOFF_HOUR + ":00)");
        }
    }

    private boolean isAzaBooking(String bookingType) {
        if (bookingType == null) {
            return false;
        }
        String normalized = bookingType.trim().toLowerCase();
        return "3aza".equals(normalized) || "aza".equals(normalized) || normalized.contains("عزاء");
    }

    private int resolveDaysCount(String bookingType) {
        return isAzaBooking(bookingType) ? AZA_DAYS_COUNT : 1;
    }

    private void validateDateRangeAvailability(
            Hall hall, LocalDate startDate, LocalDate endDate, int startHour, int endHour) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            validateBlackouts(hall, date, startHour, endHour);
            validateConflictsWithBuffer(hall, date, startHour, endHour);
        }
    }

    private void validateBlackouts(Hall hall, LocalDate date, int startHour, int endHour) {
        List<HallBlackout> blackouts = blackoutRepository.findAllByHallIdAndBlackoutDate(hall.getId(), date);
        for (HallBlackout b : blackouts) {
            if (b.getStartHour() == null || b.getEndHour() == null) {
                String msg = b.getReason() != null ? b.getReason() : "صيانة أو مناسبة رسمية";
                throw new BookingConflictException("القاعة محجوبة بالكامل في هذا التاريخ (" + date + "): " + msg);
            }
            if (startHour < b.getEndHour() && endHour > b.getStartHour()) {
                throw new BookingConflictException("الوقت المحدد يتعارض مع فترة محجوبة في (" + date + "): " + b.getReason());
            }
        }
    }

    private void validateConflictsWithBuffer(Hall hall, LocalDate date, int startHour, int endHour) {
        List<Booking> conflicts = bookingRepository.findConflictingBookingsWithBuffer(
                hall.getId(), date, startHour, endHour, hall.getBufferHours(), OffsetDateTime.now());
        if (!conflicts.isEmpty()) {
            throw new BookingConflictException("الوقت المحدد يتعارض مع حجز قائم أو فترة التجهيز الإلزامية في تاريخ " + date);
        }
    }

    private Booking initBookingFromRequest(
            Hall hall, User user, CreateBookingRequest req, int daysCount, LocalDate endDate) {
        int duration = req.getEndHour() - req.getStartHour();
        BigDecimal total = BigDecimal.valueOf((long) duration * daysCount).multiply(hall.getHourlyRate());
        return Booking.builder()
                .code(generateUniqueCode()).hall(hall).user(user)
                .fullName(req.getFullName()).nationalId(req.getNationalId()).phone(req.getPhone())
                .secondaryPhone(req.getSecondaryPhone()).email(req.getEmail())
                .bookingType(req.getBookingType()).eventDate(req.getEventDate())
                .endDate(endDate).daysCount(daysCount)
                .startHour(req.getStartHour()).endHour(req.getEndHour()).durationHours(duration)
                .appliedBufferHours(hall.getBufferHours()).hourlyRate(hall.getHourlyRate())
                .totalAmount(total)
                .currency(hall.getCurrency()).notes(req.getNotes()).build();
    }

    private Booking initBookingFromAdmin(
            Hall hall, AdminCreateBookingRequest req, int daysCount, LocalDate endDate) {
        int duration = req.getEndHour() - req.getStartHour();
        BigDecimal total = BigDecimal.valueOf((long) duration * daysCount).multiply(hall.getHourlyRate());
        return Booking.builder()
                .code(generateUniqueCode()).hall(hall).user(null)
                .fullName(req.getFullName()).nationalId(req.getNationalId()).phone(req.getPhone())
                .secondaryPhone(req.getSecondaryPhone()).email(req.getEmail())
                .bookingType(req.getBookingType()).eventDate(req.getEventDate())
                .endDate(endDate).daysCount(daysCount)
                .startHour(req.getStartHour()).endHour(req.getEndHour()).durationHours(duration)
                .appliedBufferHours(hall.getBufferHours()).hourlyRate(hall.getHourlyRate())
                .totalAmount(total)
                .currency(hall.getCurrency()).notes(req.getNotes()).build();
    }

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        String name = auth.getName();
        return userRepository.findByPhone(name)
                .or(() -> userRepository.findByEmail(name)).orElse(null);
    }

    private void validateStatusTransition(String status) {
        List<String> allowed = List.of("PENDING_PAYMENT", "CONFIRMED", "COMPLETED", "CANCELLED", "EXPIRED");
        if (!allowed.contains(status)) {
            throw new IllegalArgumentException("Invalid booking status: " + status);
        }
    }

    private String generateUniqueCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random random = new SecureRandom();
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder sb = new StringBuilder("DAR-");
            for (int i = 0; i < 6; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String code = sb.toString();
            if (bookingRepository.findByCode(code).isEmpty()) {
                return code;
            }
        }
        return "DAR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
