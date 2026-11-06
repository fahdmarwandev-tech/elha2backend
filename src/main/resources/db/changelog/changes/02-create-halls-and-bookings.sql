-- liquibase formatted sql

-- changeset abdo:02-create-halls-table
-- comment: Create halls table with capacity, pricing, operating hours, and turnaround buffer
CREATE TABLE IF NOT EXISTS halls (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(200) NOT NULL UNIQUE,
    description     TEXT,
    capacity        INT NOT NULL DEFAULT 500,
    hourly_rate     NUMERIC(10, 2) NOT NULL DEFAULT 25.00,
    currency        VARCHAR(10) NOT NULL DEFAULT 'OMR',
    buffer_hours    INT NOT NULL DEFAULT 1,
    open_hour       INT NOT NULL DEFAULT 13,
    close_hour      INT NOT NULL DEFAULT 24,
    features_json   TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE
);

-- changeset abdo:02-create-hall-media-table
-- comment: Create hall media table for gallery photos and showcases
CREATE TABLE IF NOT EXISTS hall_media (
    id              BIGSERIAL PRIMARY KEY,
    hall_id         BIGINT NOT NULL REFERENCES halls(id) ON DELETE CASCADE,
    media_url       VARCHAR(500) NOT NULL,
    title           VARCHAR(200),
    subtitle        VARCHAR(255),
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- changeset abdo:02-create-hall-blackouts-table
-- comment: Create hall blackout dates table for maintenance and private mosque closures
CREATE TABLE IF NOT EXISTS hall_blackouts (
    id              BIGSERIAL PRIMARY KEY,
    hall_id         BIGINT NOT NULL REFERENCES halls(id) ON DELETE CASCADE,
    blackout_date   DATE NOT NULL,
    start_hour      INT,
    end_hour        INT,
    reason          VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- changeset abdo:02-create-bookings-table
-- comment: Create bookings table with hourly reservation range, pricing, and voucher codes
CREATE TABLE IF NOT EXISTS bookings (
    id                      BIGSERIAL PRIMARY KEY,
    code                    VARCHAR(50) NOT NULL UNIQUE,
    hall_id                 BIGINT NOT NULL REFERENCES halls(id),
    user_id                 BIGINT REFERENCES users(id),
    full_name               VARCHAR(150) NOT NULL,
    national_id             VARCHAR(50),
    phone                   VARCHAR(30) NOT NULL,
    secondary_phone         VARCHAR(30),
    email                   VARCHAR(150),
    booking_type            VARCHAR(50) NOT NULL DEFAULT 'katbketab',
    event_date              DATE NOT NULL,
    start_hour              INT NOT NULL,
    end_hour                INT NOT NULL,
    duration_hours          INT NOT NULL,
    applied_buffer_hours    INT NOT NULL DEFAULT 1,
    hourly_rate             NUMERIC(10, 2) NOT NULL,
    total_amount            NUMERIC(10, 2) NOT NULL,
    currency                VARCHAR(10) NOT NULL DEFAULT 'OMR',
    notes                   TEXT,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING_PAYMENT',
    payment_expires_at      TIMESTAMP WITH TIME ZONE,
    created_at              TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP WITH TIME ZONE
);

-- changeset abdo:02-create-payments-table
-- comment: Create payments table for PayPal transactions and sandbox orders
CREATE TABLE IF NOT EXISTS payments (
    id                  BIGSERIAL PRIMARY KEY,
    booking_id          BIGINT NOT NULL UNIQUE REFERENCES bookings(id) ON DELETE CASCADE,
    provider            VARCHAR(50) NOT NULL DEFAULT 'PAYPAL',
    paypal_order_id     VARCHAR(100) NOT NULL UNIQUE,
    paypal_capture_id   VARCHAR(100),
    payer_email         VARCHAR(150),
    amount              NUMERIC(10, 2) NOT NULL,
    currency            VARCHAR(10) NOT NULL DEFAULT 'USD',
    status              VARCHAR(50) NOT NULL DEFAULT 'CREATED',
    paid_at             TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE
);
