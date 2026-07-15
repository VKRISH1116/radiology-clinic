package com.clinic.slot;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SlotRepository extends JpaRepository<Slot, Long> {

    boolean existsByStartTime(OffsetDateTime startTime);

    /** A day's slots: start_time in [from, to). */
    List<Slot> findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(
            OffsetDateTime from, OffsetDateTime to);

    /**
     * Load a slot with a PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE). Held for
     * the booking transaction so two people booking the same slot are serialized:
     * the second waits, then sees the first booking when it counts capacity. This
     * is what actually prevents double-booking under concurrency.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Slot s where s.id = :id")
    Optional<Slot> findByIdForUpdate(@Param("id") Long id);
}
