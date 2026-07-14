-- ============================================================
-- V1 — Initial schema (Radiology Clinic MVP)
-- Traces to docs/Architecture.html section 4.
-- Conventions:
--   money      = NUMERIC(10,2)      (never float — avoids rounding errors)
--   timestamps = TIMESTAMPTZ        (timezone-aware)
--   enums      = TEXT + CHECK       (simple and portable)
--   PKs        = BIGSERIAL          (auto-incrementing 64-bit id)
-- ============================================================

-- Users (login accounts) --------------------------------------
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,                 -- BCrypt hash, never plaintext
    role          TEXT NOT NULL CHECK (role IN ('PATIENT','STAFF','ADMIN')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Patients (a walk-in registered by staff may have no login) ---
CREATE TABLE patients (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES users(id),      -- NULLABLE: walk-ins have no account
    full_name  TEXT NOT NULL,
    phone      TEXT,
    dob        DATE,
    gender     TEXT CHECK (gender IN ('MALE','FEMALE','OTHER')),
    address    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Service catalogue (editable ultrasound studies — feature F8) --
CREATE TABLE services (
    id       BIGSERIAL PRIMARY KEY,
    category TEXT NOT NULL,
    name     TEXT NOT NULL,
    price    NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    active   BOOLEAN NOT NULL DEFAULT TRUE        -- deactivate rather than delete
);

-- Referring doctors -------------------------------------------
CREATE TABLE referring_doctors (
    id     BIGSERIAL PRIMARY KEY,
    name   TEXT NOT NULL,
    phone  TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Bookable slots (15-min grid 09:00-21:00, capacity configurable)
CREATE TABLE slots (
    id         BIGSERIAL PRIMARY KEY,
    start_time TIMESTAMPTZ NOT NULL UNIQUE,
    capacity   INT NOT NULL DEFAULT 1 CHECK (capacity > 0)
);

-- Appointments (the "visit") ----------------------------------
CREATE TABLE appointments (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    slot_id             BIGINT NOT NULL REFERENCES slots(id),
    referring_doctor_id BIGINT REFERENCES referring_doctors(id),   -- NULLABLE: no referrer
    status              TEXT NOT NULL DEFAULT 'BOOKED'
                        CHECK (status IN ('BOOKED','IN_PROGRESS','COMPLETED','CANCELLED')),
    billed_amount       NUMERIC(10,2) CHECK (billed_amount >= 0),  -- set by staff; defaults to sum of studies
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          BIGINT REFERENCES users(id)
);
CREATE INDEX idx_appointments_slot    ON appointments(slot_id);
CREATE INDEX idx_appointments_patient ON appointments(patient_id);

-- Appointment line items (price snapshot — rule BR-9) ---------
CREATE TABLE appointment_studies (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    service_id     BIGINT NOT NULL REFERENCES services(id),
    price_snapshot NUMERIC(10,2) NOT NULL CHECK (price_snapshot >= 0)  -- copied at booking time
);
CREATE INDEX idx_appt_studies_appt ON appointment_studies(appointment_id);

-- Reports (exactly one per appointment) -----------------------
CREATE TABLE reports (
    id             BIGSERIAL PRIMARY KEY,
    appointment_id BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    file_path      TEXT NOT NULL,                -- file stored outside web root
    uploaded_by    BIGINT REFERENCES users(id),
    uploaded_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Referral rules (the configurable engine — UR-A-03) ----------
-- NULL scope columns mean "matches anything"; specificity decides the winner.
CREATE TABLE referral_rules (
    id                  BIGSERIAL PRIMARY KEY,
    referring_doctor_id BIGINT REFERENCES referring_doctors(id),  -- NULL = any doctor
    service_id          BIGINT REFERENCES services(id),           -- NULL = any study
    min_amount          NUMERIC(10,2) CHECK (min_amount >= 0),    -- NULL = no lower bound
    max_amount          NUMERIC(10,2) CHECK (max_amount >= 0),    -- NULL = no upper bound
    percentage          NUMERIC(5,2) NOT NULL CHECK (percentage >= 0 AND percentage <= 100),
    active              BOOLEAN NOT NULL DEFAULT TRUE
);

-- Referral ledger (one per completed, referred visit) ---------
CREATE TABLE referrals (
    id                  BIGSERIAL PRIMARY KEY,
    appointment_id      BIGINT NOT NULL UNIQUE REFERENCES appointments(id),
    referring_doctor_id BIGINT NOT NULL REFERENCES referring_doctors(id),
    rule_id             BIGINT REFERENCES referral_rules(id),     -- which rule applied
    amount              NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    status              TEXT NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','PAID')),
    computed_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Consent records (privacy gate — NFR-02) ---------------------
CREATE TABLE consent_records (
    id              BIGSERIAL PRIMARY KEY,
    patient_id      BIGINT NOT NULL REFERENCES patients(id),
    consent_version TEXT NOT NULL,
    consented_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Audit log (accountability — NFR-03) -------------------------
CREATE TABLE audit_logs (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES users(id),
    action     TEXT NOT NULL,                    -- e.g. REPORT_DOWNLOAD, PAYOUT_UPDATE
    entity     TEXT,
    entity_id  BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
