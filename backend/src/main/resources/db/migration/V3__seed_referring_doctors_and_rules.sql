-- ============================================================
-- V3 — Seed referring doctors + illustrative referral rules
-- Gives the referral engine real rules to choose between so the
-- "most specific wins" precedence is demonstrable. Scalar subqueries
-- look ids up by name so this is independent of insertion order
-- (portable across Postgres and the H2 dev DB).
-- ============================================================

INSERT INTO referring_doctors (name, phone) VALUES
    ('Dr. Meera Sharma', '9800000001'),
    ('Dr. Arjun Iyer',   '9800000002'),
    ('Dr. Kavya Reddy',  '9800000003');

-- Doctor-specific: Dr. Meera Sharma earns 25% on any study (specificity: doctor).
INSERT INTO referral_rules (referring_doctor_id, service_id, min_amount, max_amount, percentage, active)
VALUES ((SELECT id FROM referring_doctors WHERE name = 'Dr. Meera Sharma'),
        NULL, NULL, NULL, 25.00, TRUE);

-- Service-specific: any bill including a Thyroid study pays 15% (specificity: service).
INSERT INTO referral_rules (referring_doctor_id, service_id, min_amount, max_amount, percentage, active)
VALUES (NULL, (SELECT id FROM services WHERE name = 'Ultrasound Thyroid'),
        NULL, NULL, 15.00, TRUE);

-- Amount-range: bills of 3000 and above pay 30% (specificity: amount range).
INSERT INTO referral_rules (referring_doctor_id, service_id, min_amount, max_amount, percentage, active)
VALUES (NULL, NULL, 3000.00, NULL, 30.00, TRUE);
