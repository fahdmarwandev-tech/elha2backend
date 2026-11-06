# Game3 Al-Haq (جامع الحق — دار المناسبات) — Master Architecture, Audit & Phased Implementation Plan

> **Last Updated:** 2026-11-06  
> **Project Scope:** Full-Stack Mosque Portal, Hourly Hall Reservation Engine (with Turnaround Buffer & PayPal Checkout), Religious Media Hub (YouTube + VPS Local Uploads), & CI/CD Deployment  
> **Repositories:**  
> - **Frontend:** `elha2/elha2` (Next.js 16 App Router, React 19, TypeScript, Tailwind CSS, Framer Motion — Hosted on **Cloudflare Pages**)  
> - **Backend:** `elha2-backend/elha2backend` (Spring Boot 3.3.4, Java 21, Liquibase, PostgreSQL 16, MapStruct — Co-hosted on **Production VPS** alongside `Re-Sapori-Backend`)

---

## Table of Contents

1. [Executive Summary & Current Project State](#1-executive-summary--current-project-state)
2. [Frontend Feature Inventory (Implemented vs. Pending)](#2-frontend-feature-inventory-implemented-vs-pending)
3. [Backend Audit: Mistakes, Security Flaws & Bad Practices](#3-backend-audit-mistakes-security-flaws--bad-practices)
4. [Target Architecture & Coding Restrictions (Re-Sapori Standard)](#4-target-architecture--coding-restrictions-re-sapori-standard)
5. [Core Domain Specifications](#5-core-domain-specifications)
   - [5.1 Hourly Reservation & Turnaround Buffer Engine](#51-hourly-reservation--turnaround-buffer-engine)
   - [5.2 Pricing & PayPal Payment Integration](#52-pricing--paypal-payment-integration)
   - [5.3 Hybrid Video Architecture (YouTube + VPS Local Storage)](#53-hybrid-video-architecture-youtube--vps-local-storage)
   - [5.4 Automated Oman Prayer Times & Admin Overrides](#54-automated-oman-prayer-times--admin-overrides)
   - [5.5 Email Confirmations & Password Reset OTP](#55-email-confirmations--password-reset-otp)
6. [Database Schema & Liquibase Migration Design](#6-database-schema--liquibase-migration-design)
7. [Role & Access Control Matrix](#7-role--access-control-matrix)
8. [Phased Implementation Roadmap](#8-phased-implementation-roadmap)
   - [Phase 1: Backend Foundation, Security Hardening & Liquibase Migration](#phase-1-backend-foundation-security-hardening--liquibase-migration)
   - [Phase 2: Halls, Buffer Configuration, Blackouts & Hourly Availability API](#phase-2-halls-buffer-configuration-blackouts--hourly-availability-api)
   - [Phase 3: Hourly Booking Engine, PayPal Checkout & Email Notifications](#phase-3-hourly-booking-engine-paypal-checkout--email-notifications)
   - [Phase 4: Media Hub Refactor, Prayer Times & Password Reset OTP](#phase-4-media-hub-refactor-prayer-times--password-reset-otp)
   - [Phase 5: Frontend-Backend Integration & Admin Panel Upgrades](#phase-5-frontend-backend-integration--admin-panel-upgrades)
   - [Phase 6: CI/CD Pipelines — Cloudflare Pages & Multi-Tenant VPS Deployment](#phase-6-cicd-pipelines--cloudflare-pages--multi-tenant-vps-deployment)
9. [Complete DevOps & CI/CD Reference Configurations](#9-complete-devops--cicd-reference-configurations)

---

## 1. Executive Summary & Current Project State

**Game3 Al-Haq (جامع الحق)** is the official digital platform for Al-Haqq Mosque and Community Hall (*Al-Majlis Al-Aam / Dar Al-Monasbat*) located in Seeb (Al-Mawaleh South), Sultanate of Oman.

### Current Status at a Glance

| Module / Layer | Frontend (`elha2`) | Backend (`elha2backend`) | Integration Status |
| :--- | :--- | :--- | :--- |
| **Landing Hero & Mosque Showcase** | ✅ Complete (Progressive video, convergence navbar, drawer, facilities) | 🔴 `ContentController` & `PrayersAndHistory` are 0-byte empty files | Static data in UI; hero videos currently inside `public/` |
| **Authentication (JWT + Refresh)** | ✅ Complete (`AuthForm.tsx`, proactive refresh, 401 auto-retry in `lib/api.ts`) | ✅ Implemented (`/auth/register`, `/login`, `/refresh`, `/logout`) | ✅ **Connected & Working** (Missing Password Reset OTP) |
| **Videos & Religious Media** | ✅ Complete (`YouTubePlayer.tsx`, categories, search, admin upload UI) | ⚠️ Implemented (`VideoController`, `VideoService`), but **insecure & has N+1 bugs** | ⚠️ **Connected** (Admin endpoints publicly exposed without auth!) |
| **Hall Configuration & Buffer Time** | ⚠️ Single static hall in UI; no buffer config UI yet | 🔴 `Hall`, `HallController`, `HallService`, `TimeSlot` are **0-byte empty files** | 🔴 **Disconnected** |
| **Hourly Booking Engine** | ⚠️ UI allows toggling hours, saves to browser `localStorage` | 🔴 `Booking`, `BookingController`, `BookingService` are **0-byte empty files** | 🔴 **Disconnected** (Mocked in `localStorage`) |
| **PayPal Online Payments** | 🔴 No payment checkout step in booking flow yet | 🔴 `Payment`, `PaymentController`, `PaymentService`, `PayPalConfig` are **0-byte empty files** | 🔴 **Not Implemented** |
| **Database Migrations** | N/A | 🔴 Flyway disabled (`flyway.enabled: false`), `V1__initial_schema.sql` is **0 bytes** | 🔴 Relying on unsafe `ddl-auto: update` |
| **CI/CD & Production Hosting** | 🔴 No `.github/workflows` or static export config | 🔴 No `Dockerfile`, `docker-compose.prod.yml`, or GitHub Actions workflow | 🔴 **Not Implemented** |

---

## 2. Frontend Feature Inventory (Implemented vs. Pending)

### 2.1 Implemented Frontend Features
1. **Landing Portal (`app/page.tsx`)**:
   - **Progressive Video Hero**: 3-stage loading (12 KB blurred LQ placeholder $\rightarrow$ crisp HQ first frame $\rightarrow$ auto-playing background video that halts on the final frame and fades in calligraphic *Aref Ruqaa* headings). Includes Skip (`تخطي`) and Replay controls.
   - **Scroll-Convergence Navbar**: Smooth physics over the first `240px` of scroll that shrinks link spacing and converges the navigation bar into a floating 3-bar sidebar drawer button.
   - **Frosted Glass Slide-Out Drawer**: Full navigation menu with backdrop blur (`backdrop-blur-3xl`), mosque location details, and quick actions.
   - **Three Mosque Pillars**: *Ongoing Charity (صدقة جارية)*, *Knowledge & Light (علم ونور)*, and *Cooperative Community (مجتمع متعاون)*.
2. **About Mosque & Architectural History (`components/AboutMosque.tsx`)**:
   - **Key Metrics Strip**: $9,171\,\text{m}^2$ land area, $1,800+$ worshippers capacity, $700$-person Majlis capacity, $24\text{m}$ twin minarets, $6$ gates, inaugurated in 2011.
   - **6 Detailed Facility Cards**: Main Sanctuary, Expanded Women's Musalla, Al-Majlis Al-Aam (inaugurated March 2024 with industrial kitchen & Omani halwa ovens), Admin & Surveillance Office, Men's Ablution Building (22 restrooms, 70 sinks), and Educational Programs.
   - **Future Waqf Projects Roadmap**: 2-story Quranic school, public library, commercial waqf shops, imam residences, and underground dining hall.
3. **Majlis Reservation Interface (`app/booking/page.tsx` & `components/QuickBookingModal.tsx`)**:
   - **Dual Occasion Switcher**: *Katb Ketab (عقد قران)* vs. *3aza / Condolences (حجز عزاء)*.
   - **Visual Gallery & Lightbox**: 5 architectural photos of the Majlis with fullscreen zoom modal.
   - **Interactive Shader & Fluid Lens Cursor**: `MajlisShaderBackground.tsx` and `GlassyLensCursor.tsx` providing dynamic glassmorphic reveal effects.
   - **Interactive Multi-Month Calendar & Hourly Selection**: Hour block selector (`13:00` to `24:00`), customer reservation form (Full Name, Civil ID, Phone, Secondary Phone, Email, Notes), and reservation voucher screen (`DAR-XXXXXX`).
4. **Religious Media & Streaming Hub (`app/videos/page.tsx` & `components/YouTubePlayer.tsx`)**:
   - **Unified Hybrid Player**: Parses 5 YouTube URL formats (`watch?v=`, `youtu.be/`, `embed/`, `shorts/`, raw 11-char ID) and plays locally uploaded MP4/WebM files from the backend VPS (`/uploads/videos/...`) with DOM metadata duration probing.
   - **Category Filtering & Live Search**: Filter by Friday Sermons (`khutbah`), Lessons (`lessons`), Recitations (`recitations`), Tours (`tours`), and Interviews (`interviews`).
   - **Instant YouTube Link Tester**: Preview any YouTube link on the fly without saving.
5. **Admin Dashboards (`app/admin/bookings/page.tsx` & `app/admin/videos/page.tsx`)**:
   - **Admin Bookings**: KPI counters, multi-filter table (status, occasion type, month, search), manual booking creation modal, status management, direct WhatsApp (`wa.me`) quick contact, and printable voucher.
   - **Admin Videos**: Dual-tab publisher (YouTube link auto-metadata vs. Direct File Upload up to 500 MB), Featured Video toggle, and video deletion.
6. **Authentication Client (`components/AuthForm.tsx` & `lib/api.ts`)**:
   - Login/Register with phone or email identifier, JWT decoding, proactive token refresh 60 seconds before expiry (`scheduleProactiveTokenRefresh`), `requestWithAuth` auto-retry on `401`, and custom window auth events.

### 2.2 Pending Frontend Changes Required
- **Start & End Hour Range Picker with Buffer Awareness**: Update `/booking` and `QuickBookingModal` so the user explicitly selects a **Start Hour** and **End Hour**, dynamically displaying the calculated price (`hours * hourlyRate`) and disabling hours blocked by existing bookings **plus the Admin turnaround buffer**.
- **PayPal Checkout Integration**: Add PayPal payment button/redirect flow before finalizing a reservation, showing payment status on the voucher.
- **Admin Settings Panel (`/admin/bookings`)**: Add controls for the Admin to configure the **Turnaround Buffer Time (in hours)**, **Hourly Booking Rate**, and **Hall Blackout Dates**, and connect the entire Admin Bookings page to the real backend APIs instead of `localStorage`.
- **Route Protection**: Restrict `/admin/*` pages on the client side to users with `ROLE_ADMIN`.
- **Static Export Configuration for Cloudflare Pages**: Configure `output: "export"` in `next.config.ts` and move the two heavy landing page hero videos (`1014.mp4` and `1012(1).mp4`, ~12 MB total) out of the frontend Git bundle to the backend VPS `/uploads/hero/` or CDN.

---

## 3. Backend Audit: Mistakes, Security Flaws & Bad Practices

### 3.1 Architectural Gaps (39 Empty 0-Byte Files)
Thirty-nine Java and SQL files in `elha2-backend/elha2backend` are **0 bytes**:
- **Empty Controllers (7):** `BookingController`, `HallController`, `HallBlackoutController`, `TimeSlotController`, `PaymentController`, `ContentController`, `MediaCategoryController`.
- **Empty Entities (9):** `Booking`, `Hall`, `HallBlackout`, `HallMedia`, `TimeSlot`, `Payment`, `PrayersAndHistory`, `MediaCategory`, `UserOtp`.
- **Empty Repositories (9):** `BookingRepository`, `HallRepository`, `HallBlackoutRepository`, `HallMediaRepository`, `TimeSlotRepository`, `PaymentRepository`, `PrayersAndHistoryRepository`, `MediaCategoryRepository`, `UserOtpRepository`.
- **Empty Services (7):** `BookingService`, `HallService`, `HallBlackoutService`, `TimeSlotService`, `PaymentService`, `ContentService`, `MediaCategoryService`.
- **Empty DTOs & Mappers (5):** `BookingRequests`, `BookingResponses`, `HallRequests`, `HallResponses`, `BookingMapper`, `HallMapper`, `MediaCategoryMapper`.
- **Empty Config & Migration (2):** `PayPalConfig.java`, `V1__initial_schema.sql`.

### 3.2 Critical Security Vulnerabilities
1. **Unauthenticated Admin Video Endpoints (`SecurityConfig.java` lines 37–49)**:
   - `/api/v1/videos/**` is in `PUBLIC_WHITE_LIST`. Anyone on the internet can execute `POST /api/v1/videos`, upload 500 MB files via `POST /api/v1/videos/upload` (Disk exhaustion DoS), or wipe the database via `DELETE /api/v1/videos/{id}` without logging in.
2. **Complete Absence of Role-Based Access Control (RBAC)**:
   - Zero `@PreAuthorize("hasRole('ADMIN')")` annotations exist across the codebase.
3. **Permissive Wildcard CORS with Credentials (`SecurityConfig.java` lines 97–107)**:
   - `setAllowedOriginPatterns(List.of("*"))` combined with `setAllowCredentials(true)` exposes authenticated sessions. Redundant `@CrossOrigin(origins = "*")` also exists on `VideoController`.
4. **Hardcoded JWT Secret (`application.yml` & `JwtService.java`)**:
   - Default secret `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` is hardcoded in source files instead of strictly requiring `${JWT_SECRET}`.
5. **Weak Upload Validation & Relative Path Deletion (`VideoService.java`)**:
   - Checks only filename extension string without verifying MIME type or magic bytes, and deletes files using a relative path (`Paths.get(video.getFileUrl().replaceFirst("^/", ""))`) rather than a configured base directory (`app.upload.dir`).

### 3.3 Logic Bugs, Content Mistakes & Performance Anti-Patterns
1. **Inappropriate Default Seed Video (`VideoService.java` lines 48–61)**:
   - Seeds `"السبت الممتاز | نجم الجيل | مع ويجز | THE GOATS"` (a pop music interview with Tamer Hosny and Wegz) on startup into an Islamic mosque platform.
2. **Severe N+1 Query Loop on `isFeatured` (`VideoService.java` lines 238–245 & 320–327)**:
   - Calls `videoRepository.findAll()` and loops in Java calling `videoRepository.save(v)` for each record instead of executing a single `UPDATE` query.
3. **Fragile YouTube HTML Scraping (`VideoService.java` lines 93–160)**:
   - Scrapes raw HTML from `https://www.youtube.com/watch?v=...` using regex, which fails when YouTube serves CAPTCHAs or blocks datacenter IPs.
4. **Manual Mapping & Missing MapStruct Usage**:
   - `VideoMapper.java` is written manually as a `@Component` instead of a `@Mapper(componentModel = "spring")` MapStruct interface.
5. **No Global Exception Handler (`@RestControllerAdvice`)**:
   - Controllers catch exceptions manually and return untyped `ResponseEntity<?>` with `Map.of("error", ...)`.
6. **Disabled Schema Versioning**:
   - Flyway is disabled (`flyway.enabled: false`), Liquibase is not installed, H2 console is left enabled, and Hibernate uses `ddl-auto: update`.

---

## 4. Target Architecture & Coding Restrictions (Re-Sapori Standard)

All backend code in `elha2-backend` must follow the exact layered architecture and coding restrictions established in `Re-Sapori-Backend`:

```
HTTP Request
     │
     ▼
┌─────────────────────────────┐
│  Controllers                │  ← Zero business logic; validates @Valid DTOs, delegates to Service, returns ResponseEntity<DTO>
├─────────────────────────────┤
│  Service Layer              │  ← All domain rules, buffer validation, PayPal orchestration, and transactions (@Transactional)
├─────────────────────────────┤
│  Mappers (MapStruct)        │  ← @Mapper(componentModel = "spring") interfaces for all Entity ↔ DTO conversions
├─────────────────────────────┤
│  Repositories (Spring JPA)  │  ← Data access, custom JPQL queries, @Modifying batch updates, JOIN FETCH
├─────────────────────────────┤
│  Database (PostgreSQL 16)   │  ← Schema strictly managed by Liquibase (ddl-auto: validate)
└─────────────────────────────┘
```

### Mandatory Coding Restrictions
1. **Strict Separation of Concerns**: Controllers **never** contain business logic or `try/catch` blocks. All exceptions are handled centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`).
2. **Entities Never Leave the Service Layer**: Always map Entities to DTOs using **MapStruct** (`@Mapper(componentModel = "spring")`).
3. **Constructor Injection Only**: Use Lombok `@RequiredArgsConstructor` for all Spring beans.
4. **Function Length Limit**: Every method must be **at most 20 lines of code**. Extract validation, calculation, and helper steps into descriptive `private` methods.
5. **Database & Performance Rules**:
   - **1–2 DB calls max** per endpoint.
   - **Zero N+1 queries**: Use `JOIN FETCH` or batch fetching.
   - **No DB calls inside `for` or `while` loops**: Use `@Modifying @Query` or batch operations.

---

## 5. Core Domain Specifications

### 5.1 Hourly Reservation & Turnaround Buffer Engine

#### Business Rules
1. **Selection by Hour**:
   - Users book a hall for a specific `eventDate` (`LocalDate`) by selecting a **Start Hour** (`startTime`, e.g., `16:00`) and an **End Hour** (`endTime`, e.g., `20:00`) in 1-hour increments.
   - `durationHours` is calculated as `endHour - startHour` (minimum 1 hour).
2. **Single Turnaround Buffer Time ($B$)**:
   - The Admin configures a single buffer duration in hours (`bufferHours`, e.g., `1` or `2` hours) on the Hall / System settings from the Admin Panel.
   - This single value $B$ represents the required **turnaround gap after Reservation A and before Reservation B** (for cleaning, incense/hospitality setup, and crowd dispersal).
3. **Mathematical Overlap & Buffer Formula**:
   - Let an existing active reservation $A$ span $[S_A, E_A)$ on a given hall and date, and let the turnaround buffer be $B$ hours.
   - A new proposed reservation $N$ spanning $[S_N, E_N)$ is **valid if and only if** there is at least $B$ hours between the end of the earlier reservation and the start of the later reservation:
     $$E_N + B \le S_A \quad \text{OR} \quad E_A + B \le S_N$$
   - Equivalently, reservation $N$ **conflicts** with reservation $A$ if:
     $$S_N < E_A + B \quad \text{AND} \quad E_N + B > S_A$$
   - **Example ($B = 1\text{ hour}$)**:
     - Reservation A is booked from `16:00` to `18:00` (2 hours).
     - The hour `18:00–19:00` immediately after A is the turnaround buffer after A (so Reservation B cannot start before `19:00`).
     - Similarly, if Reservation B wants to end before A starts at `16:00`, Reservation B must end at or before `15:00` so that `15:00–16:00` serves as the 1-hour buffer after B / before A.
     - Therefore, for any existing booking $[S_A, E_A)$, any candidate start hour $h \in [S_A - B, E_A + B)$ is unavailable as an active booking hour!
4. **Concurrency Safety**:
   - When creating a booking, the service acquires a pessimistic write lock (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) or checks active bookings (`status IN ('PENDING_PAYMENT', 'CONFIRMED', 'COMPLETED')` where `PENDING_PAYMENT` has not expired) in a single JPQL query to prevent double-booking race conditions.

### 5.2 Pricing & PayPal Payment Integration

#### Pricing Calculation
- Each `Hall` has an Admin-configurable `hourlyRate` (e.g. in `OMR` or `USD` for PayPal).
- Total booking amount:
  $$\text{totalAmount} = (\text{endHour} - \text{startHour}) \times \text{hourlyRate}$$
- Note: The customer is charged **only** for their reserved hours $(\text{endHour} - \text{startHour})$, **not** for the turnaround buffer hours.

#### PayPal Checkout Lifecycle (`PayPalConfig` + `PaymentService`)
1. **Step 1 — Initiate Booking & Create PayPal Order (`POST /api/v1/bookings`)**:
   - Validates hall availability (including buffer $B$ and `HallBlackout` dates).
   - Saves `Booking` with status `PENDING_PAYMENT` (holds the slot for 15 minutes via `paymentExpiresAt`) and generates reference code `DAR-XXXXXX`.
   - Calls PayPal Orders v2 API (`POST /v2/checkout/orders`) to create a PayPal order for `totalAmount`.
   - Saves a `Payment` record with `paypalOrderId` and status `CREATED`, returning the `paypalOrderId` and `approvalUrl` to the frontend.
2. **Step 2 — Capture Payment (`POST /api/v1/payments/paypal/capture`)**:
   - After user approves payment in the PayPal popup/redirect, frontend calls `/api/v1/payments/paypal/capture` with `paypalOrderId`.
   - Backend calls PayPal Capture API (`POST /v2/checkout/orders/{id}/capture`).
   - Upon `COMPLETED` status from PayPal:
     - Updates `Payment.status = COMPLETED` and records `captureId` and `paidAt`.
     - Updates `Booking.status = CONFIRMED`.
     - Triggers asynchronous Confirmation Email to the user's email address with the voucher details.
3. **Step 3 — Abandoned Payment Cleanup**:
   - A scheduled task (`@Scheduled(fixedRate = 300000)`) or lazy check marks `PENDING_PAYMENT` bookings older than 15 minutes as `EXPIRED`, freeing the time slot immediately.

### 5.3 Hybrid Video Architecture (YouTube + VPS Local Storage)

1. **Type 1 — YouTube Videos (`videoSourceType = "YOUTUBE"`)**:
   - Admin submits a YouTube URL.
   - Backend extracts the 11-character ID and queries the **free YouTube Data API v3** (`GET https://www.googleapis.com/youtube/v3/videos?id={id}&part=snippet,contentDetails,statistics&key={apiKey}`) to fetch exact ISO-8601 duration, view count, and title.
   - Falls back cleanly to **YouTube oEmbed** (`https://www.youtube.com/oembed`) if no API key is configured.
2. **Type 2 — VPS Local Uploaded Videos (`videoSourceType = "LOCAL"`)**:
   - Admin uploads an `.mp4`, `.webm`, `.mov`, or `.mkv` file via `POST /api/v1/videos/upload` (protected by `ROLE_ADMIN`).
   - Backend validates both file extension and MIME content type (`video/*`), stores the file inside the configured VPS directory (`app.upload.dir=/app/uploads/videos`, bind-mounted to `/opt/elha2-backend/prod/uploads/videos` on the host VPS), and serves it via `/uploads/videos/{filename}` with HTTP Range header support for smooth scrubbing.
3. **Landing Page Hero Videos Relocation**:
   - Move `1014.mp4` and `1012(1).mp4` out of the Next.js `public/` directory into the backend VPS `/uploads/hero/` directory (or Cloudflare R2), eliminating 12 MB of binary bloat from the frontend repository.

### 5.4 Automated Oman Prayer Times & Admin Overrides
- `ContentService` fetches daily prayer times for **Seeb / Muscat, Sultanate of Oman** (`latitude = 23.6143`, `longitude = 58.5453`, Method 4 / Umm al-Qura or Oman Ministry of Endowments tuning) via the Aladhan REST API, caching the daily schedule in memory/DB (`PrayersAndHistory`).
- Admin can optionally override specific prayer or Iqamah times via `PUT /api/v1/content/prayers` (`ROLE_ADMIN`).
- Frontend Landing Page fetches `GET /api/v1/content/prayers` to display live prayer times and automatically highlight the next upcoming prayer (`isNext`).

### 5.5 Email Confirmations & Password Reset OTP
- **Booking Confirmation Email (`MailService`)**: Uses `spring-boot-starter-mail` to send an Arabic RTL HTML email containing the reservation code (`DAR-XXXXXX`), occasion type, date, start/end hour, hall name, and payment receipt.
- **Password Reset OTP (`UserOtp`)**:
  - `POST /auth/forgot-password`: Generates a 6-digit OTP stored in `user_otps` (valid for 10 minutes) and emails it to the user.
  - `POST /auth/reset-password`: Verifies the OTP and updates the user's BCrypt password hash.

---

## 6. Database Schema & Liquibase Migration Design

Liquibase will manage all schema migrations under `src/main/resources/db/changelog/db.changelog-master.yaml` with `spring.jpa.hibernate.ddl-auto: validate`.

### Entity-Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : "has"
    USERS ||--o{ USER_OTPS : "requests"
    USERS ||--o{ BOOKINGS : "places"
    HALLS ||--o{ HALL_MEDIA : "displays"
    HALLS ||--o{ HALL_BLACKOUTS : "blocks"
    HALLS ||--o{ BOOKINGS : "hosts"
    BOOKINGS ||--o| PAYMENTS : "paid_via"
    MEDIA_CATEGORIES ||--o{ VIDEOS : "categorizes"

    USERS {
        bigserial id PK
        varchar full_name
        varchar email UK
        varchar phone UK
        varchar national_id
        varchar password_hash
        varchar role "ROLE_USER | ROLE_ADMIN"
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    HALLS {
        bigserial id PK
        varchar name
        varchar slug UK
        text description
        integer capacity
        numeric hourly_rate
        varchar currency "USD | OMR"
        integer buffer_hours "Single turnaround gap between bookings"
        integer open_hour "Default 13 (1:00 PM)"
        integer close_hour "Default 24 (12:00 AM)"
        text features_json
        boolean is_active
        timestamptz created_at
        timestamptz updated_at
    }

    HALL_MEDIA {
        bigserial id PK
        bigint hall_id FK
        varchar media_url
        varchar title
        varchar subtitle
        integer sort_order
    }

    HALL_BLACKOUTS {
        bigserial id PK
        bigint hall_id FK
        date blackout_date
        integer start_hour "Nullable (null = full day blocked)"
        integer end_hour "Nullable"
        varchar reason
        timestamptz created_at
    }

    BOOKINGS {
        bigserial id PK
        varchar code UK "DAR-XXXXXX"
        bigint hall_id FK
        bigint user_id FK "Nullable for guest/admin-created bookings"
        varchar full_name
        varchar national_id
        varchar phone
        varchar secondary_phone
        varchar email
        varchar booking_type "KATB_KETAB | AZA"
        date event_date
        integer start_hour "e.g. 16 for 16:00"
        integer end_hour "e.g. 20 for 20:00"
        integer duration_hours "end_hour - start_hour"
        integer applied_buffer_hours "Snapshot of hall buffer_hours"
        numeric hourly_rate
        numeric total_amount
        varchar currency
        text notes
        varchar status "PENDING_PAYMENT | CONFIRMED | COMPLETED | CANCELLED | EXPIRED"
        timestamptz payment_expires_at
        timestamptz created_at
        timestamptz updated_at
    }

    PAYMENTS {
        bigserial id PK
        bigint booking_id FK
        varchar provider "PAYPAL | CASH_ADMIN"
        varchar paypal_order_id UK
        varchar paypal_capture_id
        varchar payer_email
        numeric amount
        varchar currency
        varchar status "CREATED | COMPLETED | FAILED | REFUNDED"
        timestamptz paid_at
        timestamptz created_at
        timestamptz updated_at
    }

    VIDEOS {
        bigserial id PK
        bigint category_id FK
        varchar video_source_type "YOUTUBE | LOCAL"
        varchar youtube_url
        varchar youtube_id
        varchar file_url
        varchar file_name
        bigint file_size
        varchar title
        text description
        varchar category
        varchar category_name
        varchar duration
        varchar views_count
        varchar custom_poster
        boolean is_featured
        timestamptz created_at
        timestamptz updated_at
    }

    PRAYERS_AND_HISTORY {
        bigserial id PK
        varchar fajr_time
        varchar sunrise_time
        varchar dhuhr_time
        varchar asr_time
        varchar maghrib_time
        varchar isha_time
        boolean manual_override
        dateeffective_date
        timestamptz updated_at
    }
```

---

## 7. Role & Access Control Matrix

| Endpoint Pattern | HTTP Method | `GUEST` (Public) | `ROLE_USER` | `ROLE_ADMIN` | Description |
| :--- | :--- | :---: | :---: | :---: | :--- |
| `/auth/**`, `/api/auth/**` | `POST` | ✅ | ✅ | ✅ | Register, Login, Refresh, Logout, Forgot/Reset Password |
| `/api/v1/halls/**` | `GET` | ✅ | ✅ | ✅ | View halls, hourly rate, buffer setting, and hourly availability calendar |
| `/api/v1/halls/**` | `POST, PUT, DELETE` | ❌ | ❌ | ✅ | Update hall settings, **configure buffer hours**, hourly rate, & media |
| `/api/v1/blackouts/**` | `GET` | ✅ | ✅ | ✅ | View blocked dates/hours |
| `/api/v1/blackouts/**` | `POST, DELETE` | ❌ | ❌ | ✅ | Add or remove hall blackout dates/hours |
| `/api/v1/bookings/availability` | `GET` | ✅ | ✅ | ✅ | Get booked + buffered hours for a hall & month/date |
| `/api/v1/bookings` | `POST` | ✅ | ✅ | ✅ | Create a reservation & initiate PayPal order |
| `/api/v1/bookings/my` | `GET` | ❌ | ✅ | ✅ | View own reservations |
| `/api/v1/bookings/code/{code}` | `GET` | ✅ | ✅ | ✅ | Lookup booking voucher summary by reference code |
| `/api/v1/bookings/**` (Admin) | `GET, PUT, PATCH, DELETE` | ❌ | ❌ | ✅ | List all bookings, manual create, change status, delete |
| `/api/v1/payments/paypal/**` | `POST` | ✅ | ✅ | ✅ | Capture PayPal order for a pending booking |
| `/api/v1/videos/**` | `GET` | ✅ | ✅ | ✅ | Browse and watch YouTube & local sermon videos |
| `/api/v1/videos/**` | `POST, PUT, DELETE` | ❌ | ❌ | ✅ | **Admin only**: Add YouTube link, upload local MP4 to VPS, delete video |
| `/api/v1/content/**` | `GET` | ✅ | ✅ | ✅ | Fetch live Oman prayer times & mosque info |
| `/api/v1/content/**` | `PUT` | ❌ | ❌ | ✅ | Override prayer times / mosque announcements |
| `/uploads/**` | `GET` | ✅ | ✅ | ✅ | Stream uploaded video files & images from VPS volume |

---

## 8. Phased Implementation Roadmap

```
Phase 1: Foundation & Security ──► Phase 2: Halls & Buffer API ──► Phase 3: Bookings & PayPal
                                                                              │
Phase 6: CI/CD (CF Pages + VPS) ◄── Phase 5: Frontend Integration ◄── Phase 4: Videos, Prayers & Mail
```

---

### Phase 1: Backend Foundation, Security Hardening & Liquibase Migration

**Objective:** Replace the broken/insecure configuration with Re-Sapori's production-grade foundation.

1. **Dependencies (`pom.xml`)**:
   - Remove `flyway-core`, `flyway-database-postgresql`, and `h2`.
   - Add `org.liquibase:liquibase-core`, `org.springframework.boot:spring-boot-starter-mail`, and `org.springframework.boot:spring-boot-starter-actuator`.
   - Verify `mapstruct` (`1.5.5.Final`) and `lombok-mapstruct-binding` (`0.2.0`) annotation processors.
2. **Application Configuration (`src/main/resources/application.yml`)**:
   - Set `spring.jpa.hibernate.ddl-auto: validate`.
   - Enable Liquibase: `spring.liquibase.change-log: classpath:db/changelog/db.changelog-master.yaml`.
   - Remove H2 console configuration.
   - Require externalized environment variables with sensible local dev fallbacks:
     - `SERVER_PORT`, `DB_LOCAL_URL`, `DB_LOCAL_USERNAME`, `DB_LOCAL_PASSWORD`, `JWT_SECRET`.
     - `UPLOAD_DIR: ${UPLOAD_DIR:uploads}` and `UPLOAD_BASE_URL: ${UPLOAD_BASE_URL:/uploads}`.
     - `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, `PAYPAL_MODE` (`sandbox` or `live`).
     - `YOUTUBE_API_KEY` (optional, for YouTube Data API v3).
     - `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
3. **Liquibase Changelog (`db/changelog/`)**:
   - Delete the empty `src/main/resources/db/migration/V1__initial_schema.sql`.
   - Create `db/changelog/db.changelog-master.yaml` and changesets:
     - `001-create-users-and-auth-tables.yaml` (`users`, `refresh_tokens`, `user_otps`).
     - `002-create-halls-and-bookings-tables.yaml` (`halls`, `hall_media`, `hall_blackouts`, `bookings`, `payments`).
     - `003-create-media-and-content-tables.yaml` (`media_categories`, `videos`, `prayers_and_history`).
     - `004-seed-initial-data.yaml` (Seeds Default Admin account, "قاعة المجلس العام" hall with `buffer_hours = 1`, `hourly_rate = 10.00`, `open_hour = 13`, `close_hour = 24`, default media categories, and an authentic Game3 Al-Haq religious video).
4. **Global Exception Handling**:
   - Create `com.game3alhaq.exception.ErrorResponse`, `ResourceNotFoundException`, `BookingConflictException`, `PaymentProcessingException`, and `GlobalExceptionHandler` (`@RestControllerAdvice`) returning consistent HTTP status codes (`400`, `401`, `403`, `404`, `409`, `500`).
5. **SecurityConfig Hardening (`SecurityConfig.java`)**:
   - Replace wildcard `/api/v1/videos/**` in `PUBLIC_WHITE_LIST` with method-specific matchers:
     - `requestMatchers(HttpMethod.GET, "/api/v1/videos/**", "/api/v1/halls/**", "/api/v1/blackouts/**", "/api/v1/bookings/availability/**", "/api/v1/bookings/code/**", "/api/v1/content/**", "/uploads/**").permitAll()`
     - `requestMatchers(HttpMethod.POST, "/api/v1/bookings", "/api/v1/payments/paypal/**").permitAll()`
     - Protect all write/admin endpoints with `@PreAuthorize("hasRole('ADMIN')")`.
   - Configure CORS origins via `${ALLOWED_ORIGINS:http://localhost:3000}`.

---

### Phase 2: Halls, Buffer Configuration, Blackouts & Hourly Availability API

**Objective:** Implement the Hall management and Turnaround Buffer calculation engine.

1. **Entities & Repositories**:
   - Implement `Hall.java`, `HallMedia.java`, `HallBlackout.java` and their respective Spring Data JPA repositories (`HallRepository`, `HallMediaRepository`, `HallBlackoutRepository`).
2. **DTOs & MapStruct Mappers**:
   - Implement `HallRequests.java` (`UpdateHallConfigRequest` containing `hourlyRate`, `bufferHours`, `openHour`, `closeHour`, `capacity`; `CreateBlackoutRequest`) and `HallResponses.java` (`HallResponse`, `HourlyAvailabilityResponse`, `DayAvailabilitySummary`).
   - Implement `HallMapper.java` as a `@Mapper(componentModel = "spring")` interface.
3. **Hourly Availability & Buffer Calculation (`HallService` & `TimeSlotService`)**:
   - `GET /api/v1/halls`: Returns active hall details including `hourlyRate`, `bufferHours`, `openHour`, `closeHour`, and gallery images.
   - `PUT /api/v1/halls/{id}/config` (`ROLE_ADMIN`): Allows Admin to update `bufferHours` (e.g. 0, 1, 2, 3 hours), `hourlyRate`, and operating hours.
   - `GET /api/v1/bookings/availability?hallId={id}&year={YYYY}&month={MM}`:
     - Fetches all active bookings (`CONFIRMED`, `COMPLETED`, and non-expired `PENDING_PAYMENT`) and `HallBlackout` entries for the given month in **2 DB queries max**.
     - For each day, computes for every hour $h \in [\text{openHour}, \text{closeHour})$:
       - `BOOKED`: Hour falls inside an existing reservation $[S_A, E_A)$.
       - `BUFFER`: Hour falls inside $[S_A - B, S_A)$ or $[E_A, E_A + B)$ where $B$ is the turnaround buffer!
       - `BLACKOUT`: Hour is blocked by an Admin blackout.
       - `AVAILABLE`: Hour can be chosen by a user.
     - Also classifies each calendar day as `AVAILABLE`, `PARTIALLY_BOOKED`, or `FULLY_BOOKED` so the monthly calendar UI colors every day accurately.
4. **Blackout Management (`HallBlackoutController` & `HallBlackoutService`)**:
   - `GET /api/v1/blackouts`, `POST /api/v1/blackouts` (`ROLE_ADMIN`), `DELETE /api/v1/blackouts/{id}` (`ROLE_ADMIN`).

---

### Phase 3: Hourly Booking Engine, PayPal Checkout & Email Notifications

**Objective:** Replace the empty booking/payment files with a concurrency-safe hourly reservation and PayPal payment pipeline.

1. **Entities, DTOs & MapStruct Mapper**:
   - Implement `Booking.java` and `Payment.java`.
   - Implement `BookingRequests.java`:
     - `CreateBookingRequest`: `hallId`, `fullName`, `nationalId`, `phone`, `secondaryPhone`, `email`, `bookingType` (`KATB_KETAB` | `AZA`), `eventDate` (`YYYY-MM-DD`), `startHour` (`13`–`23`), `endHour` (`14`–`24`), `notes`.
     - `AdminCreateBookingRequest`: Same fields + optional `skipPayment` boolean (allows Admin to record cash/manual bookings directly as `CONFIRMED`).
     - `UpdateBookingStatusRequest`: `status` (`CONFIRMED`, `COMPLETED`, `CANCELLED`).
   - Implement `BookingResponses.java` (`BookingResponse`, `BookingInitiationResponse` with `booking`, `paypalOrderId`, `approvalUrl`).
   - Implement `BookingMapper.java` via MapStruct.
2. **Overlap + Buffer Validation Query (`BookingRepository`)**:
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
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
       Long hallId, LocalDate eventDate, int startHour, int endHour, int bufferHours, OffsetDateTime now
   );
   ```
   - This single database query enforces that **at least `bufferHours` separates the end of any earlier booking and the start of any later booking**.
3. **PayPal Client & Service (`PayPalConfig.java`, `PaymentService.java`, `PaymentController.java`)**:
   - Configure `RestClient` / `HttpClient` for PayPal REST API v2 (`/v1/oauth2/token`, `/v2/checkout/orders`, `/v2/checkout/orders/{orderId}/capture`).
   - Implement `POST /api/v1/payments/paypal/create-order` and `POST /api/v1/payments/paypal/capture/{orderId}`.
4. **Booking Controller (`BookingController.java`)**:
   - `POST /api/v1/bookings`: Public/User endpoint — validates slot + buffer, calculates `totalAmount = (endHour - startHour) * hall.getHourlyRate()`, creates `PENDING_PAYMENT` booking + PayPal order.
   - `GET /api/v1/bookings/code/{code}`: Public voucher lookup.
   - `GET /api/v1/bookings` (`ROLE_ADMIN`): Paginated/filtered list of all bookings for the Admin panel.
   - `POST /api/v1/bookings/admin` (`ROLE_ADMIN`): Direct manual booking creation by Admin.
   - `PATCH /api/v1/bookings/{id}/status` (`ROLE_ADMIN`): Update booking status (`CONFIRMED`, `COMPLETED`, `CANCELLED`).
   - `DELETE /api/v1/bookings/{id}` (`ROLE_ADMIN`): Delete booking.

---

### Phase 4: Media Hub Refactor, Prayer Times & Password Reset OTP

**Objective:** Fix all bugs in `VideoService`, implement dynamic Oman prayer times, and complete `UserOtp` & Email services.

1. **Video Service Refactor (`VideoService.java`, `VideoRepository.java`, `VideoMapper.java`)**:
   - Remove the inappropriate Tamer Hosny / Wegz seed video from `@PostConstruct`.
   - Convert `VideoMapper` to a MapStruct `@Mapper(componentModel = "spring")` interface.
   - Replace the `findAll()` loop in `createVideo` and `uploadLocalVideo` with a single repository query:
     ```java
     @Modifying
     @Query("UPDATE Video v SET v.isFeatured = false WHERE v.isFeatured = true")
     void clearAllFeaturedVideos();
     ```
   - Replace HTML regex scraping in `fetchYouTubeMetadata` with the official **YouTube Data API v3** (`videos.list` endpoint using `${YOUTUBE_API_KEY}`) + **YouTube oEmbed** fallback.
   - Use configured `${app.upload.dir}` for storing and deleting local video files on the VPS volume, and validate `MultipartFile.getContentType()` starts with `video/`.
   - Refactor all methods in `VideoService` to adhere to the **max 20 lines per function** rule.
2. **Media Categories (`MediaCategoryController`, `MediaCategoryService`, `MediaCategoryMapper`)**:
   - Implement CRUD for video categories (`khutbah`, `lessons`, `recitations`, `tours`, `interviews`).
3. **Prayer Times & Mosque Content (`ContentController`, `ContentService`, `PrayersAndHistory`)**:
   - Implement `GET /api/v1/content/prayers`: Returns today's 6 prayer times for Seeb, Oman (auto-fetched daily from Aladhan API `https://api.aladhan.com/v1/timingsByCity?city=Seeb&country=Oman&method=4` unless overridden by Admin) along with `nextPrayer` calculation.
   - Implement `PUT /api/v1/content/prayers` (`ROLE_ADMIN`): Allows Admin to manually set/override prayer times.
4. **Email & Password Reset OTP (`MailService`, `UserOtp`, `AuthService`)**:
   - Implement `MailService` (`@Async` HTML email sender for booking confirmations and OTP codes).
   - Implement `UserOtp` entity and `UserOtpRepository` with `POST /auth/forgot-password` and `POST /auth/reset-password`.

---

### Phase 5: Frontend-Backend Integration & Admin Panel Upgrades

**Objective:** Connect all Next.js screens to the Spring Boot backend and upgrade the UI for start/end hour selection, buffer settings, and PayPal checkout.

1. **API Client Expansion (`lib/api.ts`)**:
   - Add typed API functions for:
     - `getHalls()`, `updateHallConfig(id, config)`
     - `getMonthlyAvailability(hallId, year, month)`
     - `createBooking(payload)`, `capturePayPalOrder(orderId)`, `getBookingByCode(code)`
     - `getAdminBookings(filters)`, `createAdminBooking(payload)`, `updateBookingStatus(id, status)`, `deleteBooking(id)`
     - `getBlackouts(hallId)`, `createBlackout(payload)`, `deleteBlackout(id)`
     - `getPrayerTimes()`, `updatePrayerTimes(payload)`
2. **User Booking Page Upgrade (`app/booking/page.tsx` & `components/QuickBookingModal.tsx`)**:
   - Fetch real monthly availability from `GET /api/v1/bookings/availability`.
   - Replace arbitrary disconnected hour toggling with an intuitive **Start Hour $\rightarrow$ End Hour** selector (`من الساعة ... إلى الساعة ...`).
   - Display hours blocked by existing bookings or turnaround buffer as unavailable (`محجوز / فترة تجهيز`).
   - Display live price summary: `Duration (X hours) × Hourly Rate = Total Amount`.
   - Integrate **PayPal Checkout** button/redirect: when the user submits the form, initiate the booking + PayPal order, complete payment capture, and render the confirmed voucher with real backend code `DAR-XXXXXX`.
3. **Admin Bookings & Buffer Settings Upgrade (`app/admin/bookings/page.tsx`)**:
   - Remove `localStorage` (`game3_bookings`) and `INITIAL_SEED_BOOKINGS` mock dependency; load real bookings from `GET /api/v1/bookings`.
   - Add an **Admin Hall & Buffer Settings Bar / Modal** at the top of `/admin/bookings` where the Admin can set:
     - **Turnaround Buffer Time (`فترة التجهيز والفاصل الزمني بين الحجوزات`)**: e.g., `0`, `1`, `2`, `3` hours (with clear explanation: *"Added after Reservation A / before Reservation B"*).
     - **Hourly Booking Rate (`سعر الساعة`)**.
     - **Operating Hours (`ساعات العمل`)** and **Blackout Dates (`الأيام المحجوبة`)**.
4. **Admin Videos Security Integration (`app/admin/videos/page.tsx`)**:
   - Replace raw `fetch()` calls on `POST /api/v1/videos`, `POST /api/v1/videos/upload`, and `DELETE /api/v1/videos/{id}` with authenticated requests sending `Authorization: Bearer ${token}` via `getValidAccessToken()`.
5. **Hero Videos & Static Export Readiness (`app/page.tsx` & `next.config.ts`)**:
   - Configure `output: "export"` and `images: { unoptimized: true }` in `next.config.ts`.
   - Point landing page prayer times strip to `GET /api/v1/content/prayers` (with fallback to default times if offline).
   - Host `1014.mp4` and `1012(1).mp4` from `${NEXT_PUBLIC_API_URL}/uploads/hero/...` (or CDN) and remove the raw MP4 files from `public/` so the Cloudflare Pages artifact stays ultra-lightweight.

---

### Phase 6: CI/CD Pipelines — Cloudflare Pages & Multi-Tenant VPS Deployment

**Objective:** Automate deployment for both repositories using GitHub Actions, mirroring the exact architecture of `Re-Sapori` and `Re-Sapori-Backend` without port collisions.

#### Multi-Tenant VPS Port & Directory Allocation (Shared Server)

Because the production VPS already hosts `Re-Sapori-Backend` and `Mario-Juice-Backend`, `elha2-backend` must use isolated container names, deployment paths, and host ports:

| Service / Stack | VPS Directory | Backend Container & Host Port | Postgres Container & Host Port | Upload Volume |
| :--- | :--- | :--- | :--- | :--- |
| **Re-Sapori** (Existing) | `/opt/app/production` | `resapori-backend` (`8080:8080`) | `resapori-db` (`127.0.0.1:5432:5432`) | `/opt/app/production/uploads` |
| **Mario Juice** (Existing) | `/opt/mario-backend/prod` | `mariojuice-backend` (`8081:8080`) | `mariojuice-db` (`127.0.0.1:5433:5432`) | `/opt/mario-backend/prod/uploads` |
| **Game3 Al-Haq (`elha2`)** (New) | `/opt/elha2-backend/prod` | `elha2-backend` (`8082:8080`) | `elha2-db` (`127.0.0.1:5434:5432`) | `/opt/elha2-backend/prod/uploads` |

---

## 9. Complete DevOps & CI/CD Reference Configurations

### 9.1 Backend `Dockerfile` (`elha2-backend/elha2backend/Dockerfile`)

```dockerfile
# Stage 1: Build the JAR with Maven
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /app

# Copy Maven wrapper and POM first (to cache dependencies)
COPY mvnw mvnw.cmd pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src src
RUN ./mvnw clean package -DskipTests

# Stage 2: Minimal Runtime Image
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# Non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
RUN mkdir -p /app/uploads/videos /app/uploads/hero && chown -R spring:spring /app/uploads
USER spring:spring

# Copy built jar from builder
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 9.2 Backend `docker-compose.prod.yml` (`elha2-backend/elha2backend/docker-compose.prod.yml`)

```yaml
services:
  db:
    image: postgres:16-alpine
    container_name: elha2-db
    restart: always
    environment:
      POSTGRES_USER: ${POSTGRES_USER:-postgres}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-postgres}
      POSTGRES_DB: ${POSTGRES_DB:-game3alhaq_db}
    ports:
      - "127.0.0.1:${DB_PORT:-5434}:5432"
    volumes:
      - elha2_pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres} -d ${POSTGRES_DB:-game3alhaq_db}"]
      interval: 5s
      timeout: 5s
      retries: 5

  backend:
    image: ghcr.io/${GITHUB_REPOSITORY:-abo-hashish/elha2-backend}:latest
    container_name: elha2-backend
    restart: always
    env_file:
      - .env
    ports:
      - "${SERVER_PORT:-8082}:8080"
    volumes:
      - ./uploads:/app/uploads
    depends_on:
      db:
        condition: service_healthy

volumes:
  elha2_pgdata:
```

### 9.3 Backend GitHub Actions Workflow (`.github/workflows/deploy-production.yml`)

```yaml
name: Build & Deploy Production

on:
  push:
    branches:
      - main
  workflow_dispatch:

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GitHub Container Registry (GHCR)
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Downcase repository name for docker tag
        id: repo_downcase
        run: |
          echo "IMAGE_NAME=ghcr.io/${GITHUB_REPOSITORY,,}" >> $GITHUB_OUTPUT

      - name: Build and push production image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ steps.repo_downcase.outputs.IMAGE_NAME }}:latest
            ${{ steps.repo_downcase.outputs.IMAGE_NAME }}:${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    needs: build-and-push
    runs-on: ubuntu-latest
    steps:
      - name: Deploy to Production VPS over SSH
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          key: ${{ secrets.SSH_KEY }}
          port: ${{ secrets.SSH_PORT || 22 }}
          script: |
            set -e
            echo "${{ secrets.GITHUB_TOKEN }}" | docker login ghcr.io -u "${{ github.actor }}" --password-stdin
            cd /opt/elha2-backend/prod
            docker compose pull backend
            docker compose up -d --remove-orphans
            docker image prune -f
```

### 9.4 Frontend `next.config.ts` (`elha2/elha2/next.config.ts`)

```typescript
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "export",
  reactStrictMode: true,
  images: {
    unoptimized: true,
    remotePatterns: [
      {
        protocol: "https",
        hostname: "**",
      },
      {
        protocol: "http",
        hostname: "localhost",
      },
    ],
  },
};

export default nextConfig;
```

### 9.5 Frontend GitHub Actions Workflow (`elha2/elha2/.github/workflows/deploy.yml`)

```yaml
name: Deploy Frontend to Cloudflare Pages

on:
  push:
    branches:
      - main
      - dev
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    name: Build & Deploy
    steps:
      - name: Checkout Repository
        uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: 'npm'

      - name: Install Dependencies
        run: npm ci

      - name: Build Static Export
        env:
          NEXT_PUBLIC_API_URL: ${{ github.ref_name == 'dev' && (vars.NEXT_PUBLIC_API_URL_DEV || secrets.NEXT_PUBLIC_API_URL_DEV) || (vars.NEXT_PUBLIC_API_URL || secrets.NEXT_PUBLIC_API_URL) }}
        run: npm run build

      - name: Create Cloudflare Pages Project
        continue-on-error: true
        uses: cloudflare/wrangler-action@v3
        with:
          apiToken: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          accountId: ${{ secrets.CLOUDFLARE_ACCOUNT_ID || vars.CLOUDFLARE_ACCOUNT_ID }}
          command: pages project create elha2 --production-branch main

      - name: Deploy to Cloudflare Pages
        uses: cloudflare/wrangler-action@v3
        with:
          apiToken: ${{ secrets.CLOUDFLARE_API_TOKEN }}
          accountId: ${{ secrets.CLOUDFLARE_ACCOUNT_ID || vars.CLOUDFLARE_ACCOUNT_ID }}
          command: pages deploy out --project-name=elha2 --branch=${{ github.ref_name }} --commit-dirty=true
```

### 9.6 Nginx Reverse Proxy Snippet on VPS (`/etc/nginx/sites-available/api.game3alhaq.org`)

```nginx
server {
    listen 80;
    server_name api.game3alhaq.org;

    # Allow 500MB video uploads from Admin panel
    client_max_body_size 550M;
    proxy_read_timeout 300s;
    proxy_connect_timeout 75s;
    proxy_send_timeout 300s;

    location / {
        proxy_pass http://127.0.0.1:8082;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
*(Followed by `sudo certbot --nginx -d api.game3alhaq.org` to enable HTTPS).*
