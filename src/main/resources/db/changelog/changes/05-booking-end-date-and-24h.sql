-- liquibase formatted sql

-- changeset abdo:05-add-bookings-end-date-and-days-count
-- comment: Support multi-day bookings (3 consecutive days for 3aza) and 24-hour hall availability
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS end_date DATE;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS days_count INT NOT NULL DEFAULT 1;

UPDATE bookings SET end_date = event_date WHERE end_date IS NULL;
UPDATE bookings SET days_count = 3, end_date = event_date + INTERVAL '2 days'
WHERE LOWER(booking_type) IN ('3aza', 'aza') AND days_count = 1;

-- changeset abdo:05-update-halls-24-hour-operating-window
-- comment: Open hall 24 hours (0 to 24) for Katb Ketab while enforcing Maghrib cutoff for 3aza in service layer
ALTER TABLE halls ALTER COLUMN open_hour SET DEFAULT 0;
ALTER TABLE halls ALTER COLUMN close_hour SET DEFAULT 24;
UPDATE halls SET open_hour = 0, close_hour = 24;
