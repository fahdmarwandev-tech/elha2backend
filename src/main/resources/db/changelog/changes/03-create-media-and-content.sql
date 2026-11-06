-- liquibase formatted sql

-- changeset abdo:03-create-media-categories-table
-- comment: Create media categories table
CREATE TABLE IF NOT EXISTS media_categories (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(255),
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- changeset abdo:03-create-videos-table
-- comment: Create videos table for YouTube and local MP4 uploads
CREATE TABLE IF NOT EXISTS videos (
    id                  BIGSERIAL PRIMARY KEY,
    category_id         BIGINT REFERENCES media_categories(id),
    video_source_type   VARCHAR(20) NOT NULL DEFAULT 'YOUTUBE',
    youtube_url         VARCHAR(500),
    youtube_id          VARCHAR(50),
    file_url            VARCHAR(500),
    file_name           VARCHAR(255),
    file_size           BIGINT,
    title               VARCHAR(255) NOT NULL,
    description         TEXT,
    category            VARCHAR(50) DEFAULT 'lessons',
    category_name       VARCHAR(100) DEFAULT 'دروس ومواعظ',
    duration            VARCHAR(30),
    views_count         VARCHAR(50),
    custom_poster       VARCHAR(500),
    is_featured         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE
);

-- changeset abdo:03-create-prayers-and-history-table
-- comment: Create prayers and history table for Oman prayer timings and mosque chronicle
CREATE TABLE IF NOT EXISTS prayers_and_history (
    id                  BIGSERIAL PRIMARY KEY,
    fajr_time           VARCHAR(20),
    sunrise_time        VARCHAR(20),
    dhuhr_time          VARCHAR(20),
    asr_time            VARCHAR(20),
    maghrib_time        VARCHAR(20),
    isha_time           VARCHAR(20),
    manual_override     BOOLEAN NOT NULL DEFAULT FALSE,
    effective_date      DATE,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE
);
