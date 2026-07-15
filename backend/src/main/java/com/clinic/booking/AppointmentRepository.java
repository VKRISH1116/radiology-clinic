package com.clinic.booking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    /** How many live (non-cancelled) appointments a slot already holds. */
    long countBySlotIdAndStatusNot(Long slotId, AppointmentStatus status);

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
}
