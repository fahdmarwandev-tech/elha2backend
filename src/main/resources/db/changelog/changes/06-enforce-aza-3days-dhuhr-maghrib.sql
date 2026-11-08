-- liquibase formatted sql

-- changeset abdo:06-enforce-aza-3-days-dhuhr-to-maghrib
-- comment: Ensure all 3aza bookings in database reserve 3 consecutive days from Dhuhr (12:00) to Maghrib (18:00)
UPDATE bookings
SET days_count = 3,
    end_date = event_date + INTERVAL '2 days',
    start_hour = 12,
    end_hour = 18,
    duration_hours = 6
WHERE LOWER(booking_type) IN ('3aza', 'aza') OR booking_type LIKE '%عزاء%';

ALTER TABLE bookings
DROP CONSTRAINT IF EXISTS chk_bookings_aza_3days_dhuhr_maghrib;

ALTER TABLE bookings
ADD CONSTRAINT chk_bookings_aza_3days_dhuhr_maghrib
CHECK (
    LOWER(booking_type) NOT IN ('3aza', 'aza')
    OR (
        days_count = 3
        AND end_date = event_date + INTERVAL '2 days'
        AND start_hour = 12
        AND end_hour = 18
        AND duration_hours = 6
    )
);
