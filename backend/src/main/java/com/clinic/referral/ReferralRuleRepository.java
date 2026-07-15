package com.clinic.referral;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRuleRepository extends JpaRepository<ReferralRule, Long> {

    /** All active rules in id order — the candidate set the engine scores per visit. */
    List<ReferralRule> findByActiveTrueOrderByIdAsc();

    List<ReferralRule> findAllByOrderByIdAsc();
}
