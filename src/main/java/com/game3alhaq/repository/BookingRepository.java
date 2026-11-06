package com.game3alhaq.repository;

import com.game3alhaq.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByCode(String code);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.hall h
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.payment p
        WHERE b.code = :code
    """)
    Optional<Booking> findByCodeWithDetails(@Param("code") String code);

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.hall h
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.payment p
        WHERE b.id = :id
    """)
    Optional<Booking> findByIdWithDetails(@Param("id") Long id);

    List<Booking> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findAllByStatusAndPaymentExpiresAtBefore(String status, OffsetDateTime cutoff);

    @Query("SELECT b FROM Booking b WHERE b.hall.id = :hallId AND b.eventDate BETWEEN :start AND :end AND (b.status IN ('CONFIRMED', 'COMPLETED') OR (b.status = 'PENDING_PAYMENT' AND b.paymentExpiresAt > :now))")
    List<Booking> findActiveBookingsInDateRange(
            @Param("hallId") Long hallId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("now") OffsetDateTime now
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.hall.id = :hallId
          AND b.eventDate = :eventDate
          AND (
               b.status IN ('CONFIRMED', 'COMPLETED')
               OR (b.status = 'PENDING_PAYMENT' AND b.paymentExpiresAt > :now)
          )
          AND (:startHour < (b.endHour + :bufferHours) AND (:endHour + :bufferHours) > b.startHour)
    """)
    List<Booking> findConflictingBookingsWithBuffer(
            @Param("hallId") Long hallId,
            @Param("eventDate") LocalDate eventDate,
            @Param("startHour") int startHour,
            @Param("endHour") int endHour,
            @Param("bufferHours") int bufferHours,
            @Param("now") OffsetDateTime now
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.hall h
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.payment p
        WHERE (:status IS NULL OR b.status = :status)
          AND (:bookingType IS NULL OR b.bookingType = :bookingType)
          AND (cast(:fromDate as date) IS NULL OR b.eventDate >= :fromDate)
          AND (cast(:toDate as date) IS NULL OR b.eventDate <= :toDate)
        ORDER BY b.eventDate DESC, b.startHour ASC
    """)
    List<Booking> findAdminBookings(
            @Param("status") String status,
            @Param("bookingType") String bookingType,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.hall h
        LEFT JOIN FETCH b.user u
        LEFT JOIN FETCH b.payment p
        WHERE (b.user IS NOT NULL AND b.user.id = :userId)
           OR b.phone = :contact
           OR b.email = :contact
        ORDER BY b.createdAt DESC
    """)
    List<Booking> findUserBookings(
            @Param("userId") Long userId,
            @Param("contact") String contact
    );

    @Modifying
    @Query("UPDATE Booking b SET b.status = 'EXPIRED' WHERE b.status = 'PENDING_PAYMENT' AND b.paymentExpiresAt < :cutoff")
    int markExpiredBookings(@Param("cutoff") OffsetDateTime cutoff);
}
