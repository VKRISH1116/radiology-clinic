package com.clinic.referral;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The referral-calculation engine. Given a visit (its referring doctor, the
 * studies on it, and the bill), it finds the single winning rule and the payout.
 *
 * Rules filter by scope (doctor / service / amount range); a NULL scope column
 * means "any". Several rules can match at once, so precedence is decided by a
 * SPECIFICITY score — the more constrained a rule, the higher it scores:
 *   doctor +4, service +2, amount-range +1.
 * Highest score wins; a tie breaks to the first rule in the list, so passing rules
 * in id order makes it deterministic (the default 20% rule, id 1, is the ultimate
 * fallback). This yields the intended order doctor+service &gt; doctor &gt; service
 * &gt; range &gt; default.
 *
 * The core {@link #select} method is pure (rules passed in) so it unit-tests
 * without a database.
 */
@Service
public class ReferralEngine {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ReferralRuleRepository ruleRepository;

    public ReferralEngine(ReferralRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /** Evaluate against the active rules in the database (ordered by id for determinism). */
    @Transactional(readOnly = true)
    public Optional<ReferralOutcome> evaluate(Long doctorId, Set<Long> serviceIds, BigDecimal bill) {
        return select(ruleRepository.findByActiveTrueOrderByIdAsc(), doctorId, serviceIds, bill);
    }

    /**
     * Pure selection: pick the most specific matching rule and compute the payout.
     * On a tie the first matching rule wins, so callers pass rules in id order.
     */
    public Optional<ReferralOutcome> select(
            List<ReferralRule> rules, Long doctorId, Set<Long> serviceIds, BigDecimal bill) {
        ReferralRule best = null;
        int bestScore = -1;
        for (ReferralRule rule : rules) {
            if (!matches(rule, doctorId, serviceIds, bill)) {
                continue;
            }
            int score = specificity(rule);
            if (best == null || score > bestScore) {
                best = rule;
                bestScore = score;
            }
        }
        if (best == null) {
            return Optional.empty();
        }
        BigDecimal amount = bill.multiply(best.getPercentage())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return Optional.of(new ReferralOutcome(best, amount));
    }

    static boolean matches(ReferralRule rule, Long doctorId, Set<Long> serviceIds, BigDecimal bill) {
        if (rule.getReferringDoctorId() != null && !rule.getReferringDoctorId().equals(doctorId)) {
            return false;
        }
        if (rule.getServiceId() != null && !serviceIds.contains(rule.getServiceId())) {
            return false;
        }
        if (rule.getMinAmount() != null && bill.compareTo(rule.getMinAmount()) < 0) {
            return false;
        }
        if (rule.getMaxAmount() != null && bill.compareTo(rule.getMaxAmount()) > 0) {
            return false;
        }
        return true;
    }

    static int specificity(ReferralRule rule) {
        int score = 0;
        if (rule.getReferringDoctorId() != null) {
            score += 4;
        }
        if (rule.getServiceId() != null) {
            score += 2;
        }
        if (rule.getMinAmount() != null || rule.getMaxAmount() != null) {
            score += 1;
        }
        return score;
    }

    /** The winning rule and the money it yields for this bill. */
    public record ReferralOutcome(ReferralRule rule, BigDecimal amount) {
    }
}
