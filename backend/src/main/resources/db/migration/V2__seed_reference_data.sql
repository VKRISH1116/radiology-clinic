-- ============================================================
-- V2 — Seed reference data
-- The ultrasound service catalogue (URD Appendix A) and the
-- default referral rule (PRD OQ-03: 20% of the bill).
-- ============================================================

-- Service catalogue (prices in INR) ---------------------------
INSERT INTO services (category, name, price) VALUES
    ('General',    'Ultrasound Abdomen & Pelvis', 1000),
    ('General',    'Ultrasound Pelvis',           1000),
    ('General',    'Ultrasound Small Parts',      1000),
    ('General',    'Ultrasound Thyroid',          1500),
    ('General',    'Ultrasound Scrotum',          1500),
    ('General',    'Ultrasound Doppler',          1000),
    ('Obstetrics', 'NT Scan',                     1000),
    ('Obstetrics', 'TIFFA Scan',                  2000),
    ('Obstetrics', 'Growth & Doppler Scan',       2000);

-- Default referral rule: 20% for any doctor, any study, any amount.
-- Its all-NULL scope makes it the least specific → the fallback winner.
INSERT INTO referral_rules (referring_doctor_id, service_id, min_amount, max_amount, percentage, active)
VALUES (NULL, NULL, NULL, NULL, 20.00, TRUE);
