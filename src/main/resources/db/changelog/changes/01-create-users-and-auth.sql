-- liquibase formatted sql

-- changeset abdo:01-create-users-table
-- comment: Create users table with authentication and role fields
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    full_name       VARCHAR(150) NOT NULL,
    email           VARCHAR(150) UNIQUE,
    phone           VARCHAR(30) NOT NULL UNIQUE,
    national_id     VARCHAR(50),
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE
);

-- changeset abdo:01-create-refresh-tokens-table
-- comment: Create refresh tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(500) NOT NULL UNIQUE,
    expiry_date     TIMESTAMP WITH TIME ZONE NOT NULL,
    is_revoked      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- changeset abdo:01-create-user-otps-table
-- comment: Create user OTPs table for password reset
CREATE TABLE IF NOT EXISTS user_otps (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users(id) ON DELETE CASCADE,
    identifier      VARCHAR(150) NOT NULL,
    otp_code        VARCHAR(10) NOT NULL,
    purpose         VARCHAR(50) NOT NULL DEFAULT 'PASSWORD_RESET',
    expiry_date     TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
