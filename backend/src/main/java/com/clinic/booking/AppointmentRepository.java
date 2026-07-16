package com.clinic.booking;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** How many live (non-cancelled) appointments a slot already holds. */
    long countBySlotIdAndStatusNot(Long slotId, AppointmentStatus status);

    /**
     * A patient's appointments, newest first, with their studies eagerly fetched
     * in one query (left join fetch) so building the response doesn't trigger an
     * extra studies query per appointment (an N+1). distinct de-dupes the parent
     * rows the join multiplies.
     */
    @Query("""
            select distinct a from Appointment a
            left join fetch a.studies
            where a.patientId = :patientId
            order by a.id desc
            """)
    List<Appointment> findByPatientIdWithStudies(@Param("patientId") Long patientId);

    /** Every appointment with its studies fetched — the staff schedule (small clinic). */
    @Query("""
            select distinct a from Appointment a
            left join fetch a.studies
            order by a.slotId
            """)
    List<Appointment> findAllWithStudies();

    /**
     * Live appointment counts for many slots at once, as [slotId, count] rows.
     * One grouped query instead of one-per-slot (avoids an N+1 when listing a day).
     */
    @Query("""
            select a.slotId, count(a)
            from Appointment a
            where a.slotId in :slotIds and a.status <> com.clinic.booking.AppointmentStatus.CANCELLED
            group by a.slotId
            """)
    List<Object[]> countActiveGroupedBySlotId(@Param("slotIds") List<Long> slotIds);

    /**
     * Live (non-cancelled) appointments whose slot falls in [from, to) — used for
     * the admin "today's appointments" KPI. Cross-joins Slot by id (Appointment
     * holds slot_id as a scalar, not a JPA association) so we can filter on the
     * slot's start_time.
     */
    @Query("""
            select count(a) from Appointment a, Slot s
            where a.slotId = s.id
              and s.startTime >= :from and s.startTime < :to
              and a.status <> com.clinic.booking.AppointmentStatus.CANCELLED
            """)
    long countActiveInSlotWindow(
            @Param("from") OffsetDateTime from, @Param("to") OffsetDateTime to);

    /**
     * Completed visits that still have no report on file — the "reports pending"
     * KPI. Uses a correlated NOT EXISTS against the Report entity.
     */
    @Query("""
            select count(a) from Appointment a
            where a.status = com.clinic.booking.AppointmentStatus.COMPLETED
              and not exists (select 1 from Report r where r.appointmentId = a.id)
            """)
    long countCompletedWithoutReport();
}
