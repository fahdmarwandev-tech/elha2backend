-- liquibase formatted sql

-- changeset abdo:08-add-payment-receipt-and-order-status-to-bookings
-- comment: Add payment receipt image URL and order status columns to bookings table
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS payment_receipt_image_url TEXT;
ALTER TABLE bookings ADD COLUMN IF NOT EXISTS order_status VARCHAR(50) DEFAULT 'PENDING_PAYMENT';
ALTER TABLE bookings ALTER COLUMN payment_receipt_image_url TYPE TEXT;

UPDATE bookings SET order_status = status WHERE order_status IS NULL;
