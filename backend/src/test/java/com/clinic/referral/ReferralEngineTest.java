package com.clinic.referral;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.clinic.referral.ReferralEngine.ReferralOutcome;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Pure unit tests of the selection algorithm — no Spring, no database. Rules are
 * passed in directly, in id order (as the DB query returns them). The default 20%
 * rule (all-NULL scope) is always present, mirroring the seed data.
 */
class ReferralEngineTest {

    private final ReferralEngine engine = new ReferralEngine(null); // repo unused by select()

    // Scope fields are (doctorId, serviceId, min, max); null means "any".
    private static ReferralRule rule(Long doctorId, Long serviceId, String min, String max, String pct) {
        ReferralRule r = new ReferralRule();
        r.setReferringDoctorId(doctorId);
        r.setServiceId(serviceId);
        r.setMinAmount(min == null ? null : new BigDecimal(min));
        r.setMaxAmount(max == null ? null : new BigDecimal(max));
        r.setPercentage(new BigDecimal(pct));
        r.setActive(true);
        return r;
    }

    private static final ReferralRule DEFAULT = rule(null, null, null, null, "20");
    private static final ReferralRule DOCTOR_1 = rule(1L, null, null, null, "25");
    private static final ReferralRule SERVICE_4 = rule(null, 4L, null, null, "15");
    private static final ReferralRule RANGE_3000 = rule(null, null, "3000", null, "30");
    private static final ReferralRule DOCTOR_1_SERVICE_4 = rule(1L, 4L, null, null, "10");

    private BigDecimal amountFor(List<ReferralRule> rules, Long doctorId, Set<Long> services, String bill) {
        Optional<ReferralOutcome> outcome = engine.select(rules, doctorId, services, new BigDecimal(bill));
        assertTrue(outcome.isPresent(), "expected a matching rule");
        return outcome.get().amount();
    }

    @Test
    void onlyDefaultMatches_usesDefaultPercentage() {
        // Doctor with no specific rule, cheap bill: only the default 20% applies.
        assertEquals(new BigDecimal("200.00"),
                amountFor(List.of(DEFAULT, DOCTOR_1, SERVICE_4, RANGE_3000), 9L, Set.of(1L), "1000"));
    }

    @Test
    void doctorRule_beatsDefault() {
        assertEquals(new BigDecimal("250.00"),
                amountFor(List.of(DEFAULT, DOCTOR_1, SERVICE_4, RANGE_3000), 1L, Set.of(1L), "1000"));
    }

    @Test
    void serviceRule_beatsDefault_evenThoughItsPercentageIsLower() {
        // The 15% service rule is MORE SPECIFIC than the 20% default, so it wins.
        assertEquals(new BigDecimal("225.00"),
                amountFor(List.of(DEFAULT, DOCTOR_1, SERVICE_4, RANGE_3000), 9L, Set.of(4L), "1500"));
    }

    @Test
    void doctorPlusServiceRule_beatsDoctorOnly() {
        // doctor+service (score 6) outranks doctor-only (score 4).
        assertEquals(new BigDecimal("100.00"),
                amountFor(List.of(DEFAULT, DOCTOR_1, SERVICE_4, DOCTOR_1_SERVICE_4), 1L, Set.of(4L), "1000"));
    }

    @Test
    void amountRangeRule_appliesWithinBounds() {
        assertEquals(new BigDecimal("1200.00"),
                amountFor(List.of(DEFAULT, RANGE_3000), 9L, Set.of(1L), "4000"));
    }

    @Test
    void amountRangeRule_ignoredBelowItsMinimum() {
        // 2999 is below the 3000 floor, so it falls back to the default 20%.
        assertEquals(new BigDecimal("599.80"),
                amountFor(List.of(DEFAULT, RANGE_3000), 9L, Set.of(1L), "2999"));
    }

    @Test
    void noMatchingRule_returnsEmpty() {
        // Only a rule for doctor 1 exists; doctor 2 matches nothing (no default here).
        Optional<ReferralOutcome> outcome =
                engine.select(List.of(DOCTOR_1), 2L, Set.of(1L), new BigDecimal("1000"));
        assertTrue(outcome.isEmpty());
    }

    @Test
    void payout_isRoundedHalfUpToTwoDecimals() {
        // 333.35 * 10% = 33.335 -> HALF_UP -> 33.34
        assertEquals(new BigDecimal("33.34"),
                amountFor(List.of(rule(null, null, null, null, "10")), 1L, Set.of(1L), "333.35"));
    }
}
