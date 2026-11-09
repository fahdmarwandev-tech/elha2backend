-- liquibase formatted sql

-- changeset abdo:07-create-staff-emails-table
-- comment: Create staff_emails table for mosque administration and notifications
CREATE TABLE IF NOT EXISTS staff_emails (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(150) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- changeset abdo:07-seed-initial-staff-emails
-- comment: Seed default mosque staff email addresses
INSERT INTO staff_emails (full_name, email)
VALUES
    ('مدير النظام الرئيسي', 'admin@game3alhaq.com'),
    ('مسؤول الحجوزات والمناسبات', 'marwanyasserhassan7@gmail.com')
ON CONFLICT (email) DO NOTHING;
