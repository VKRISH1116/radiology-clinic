package com.clinic.referral;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReferralRepository extends JpaRepository<Referral, Long> {

    /** A visit is only paid once (appointment_id is UNIQUE); used to stay idempotent. */
    boolean existsByAppointmentId(Long appointmentId);

    List<Referral> findAllByOrderByComputedAtDesc();

    /**
     * Payout summary per doctor: [referringDoctorId, count, totalAmount], biggest
     * payout first. This is the "top referrers" report (PRD Should-have).
     */
    @Query("""
            select r.referringDoctorId, count(r), coalesce(sum(r.amount), 0)
            from Referral r
            group by r.referringDoctorId
            order by sum(r.amount) desc
            """)
    List<Object[]> summariseByDoctor();
}
