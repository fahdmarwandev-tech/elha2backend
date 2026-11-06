-- liquibase formatted sql

-- changeset abdo:04-seed-admin-user
-- comment: Seed default system administrator user (idempotent)
INSERT INTO users (full_name, email, phone, national_id, password_hash, role, is_active, created_at)
SELECT 'مدير النظام', 'admin@game3alhaq.com', '+96890000000', '10000000', '$2a$10$9b3DnQwh8WP1fE.tGRSTveSGzwIVmnmFsgvBj6XL/K7fZtSqsRNL2', 'ROLE_ADMIN', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE email = 'admin@game3alhaq.com' OR phone = '+96890000000'
);

-- changeset abdo:04-seed-default-hall
-- comment: Seed the default main hall (Al-Majlis Al-Aam)
INSERT INTO halls (name, slug, description, capacity, hourly_rate, currency, buffer_hours, open_hour, close_hour, features_json, is_active, created_at)
SELECT 'قاعة المجلس العام', 'al-majlis-al-aam', 'دار المناسبات وقاعة المجلس العام بجامع الحق - سيب - الموالح الجنوبية، مجهزة بأحدث الخدمات والضيافة والتكييف المركزي ومطبخ صناعي متكامل.', 700, 10.00, 'OMR', 1, 13, 24, '["مكيف مركزي","سماعات رقمية وشاشات عرض","مطبخ مجهز وفرن حلوى عمانية","مواقف سيارات واسعة","مدخل مستقل للضيافة"]', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM halls WHERE slug = 'al-majlis-al-aam'
);

-- changeset abdo:04-seed-media-categories
-- comment: Seed default media categories for videos
INSERT INTO media_categories (name, slug, description, sort_order, is_active, created_at)
VALUES
    ('دروس ومواعظ', 'lessons', 'الدروس والمحاضرات الدينية والمواعظ المنعقدة في جامع الحق', 1, TRUE, CURRENT_TIMESTAMP),
    ('خطب الجمعة', 'khutbah', 'تسجيلات خطب صلاة الجمعة', 2, TRUE, CURRENT_TIMESTAMP),
    ('تلاوات خاشعة', 'recitations', 'تلاوات قرآنية وتراويح من محراب جامع الحق', 3, TRUE, CURRENT_TIMESTAMP),
    ('جولات وتعريف بالجامع', 'tours', 'جولات مرئية للتعريف بمرافق جامع الحق ودار المناسبات', 4, TRUE, CURRENT_TIMESTAMP),
    ('لقاءات وفعاليات', 'interviews', 'لقاءات وفعاليات ملتقى جامع الحق والمجتمع', 5, TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (slug) DO NOTHING;

-- changeset abdo:04-seed-default-video
-- comment: Seed default featured video
INSERT INTO videos (video_source_type, youtube_url, youtube_id, title, description, category, category_name, duration, views_count, is_featured, created_at)
SELECT 'YOUTUBE', 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', 'dQw4w9WgXcQ', 'التعريف بجامع الحق والمجلس العام بالموالح الجنوبية', 'نبذة تعريفية عن جامع الحق ودور مرافقه في خدمة مجتمع الموالح وسلطنة عمان', 'tours', 'جولات وتعريف بالجامع', '05:00', '1.2K', TRUE, CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM videos WHERE youtube_id = 'dQw4w9WgXcQ'
);
