package com.clinic.slot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owns the bookable-slot grid: 15-minute slots from 09:00 up to 21:00, in the
 * clinic's local timezone (48 slots/day). Slots are generated lazily the first
 * time a day is requested, so we don't pre-fill the table for every future date.
 *
 * This service knows nothing about appointments — availability (how many of a
 * slot's seats are taken) is a booking concern, computed in the booking layer.
 */
@Service
public class SlotService {

    // The clinic is in India; book against its wall-clock time, not the server's.
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalTime OPEN = LocalTime.of(9, 0);
    private static final LocalTime CLOSE = LocalTime.of(21, 0);
    private static final int SLOT_MINUTES = 15;
    private static final int DEFAULT_CAPACITY = 1;

    private final SlotRepository slotRepository;

    public SlotService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    /**
     * Make sure a day's slots exist, creating any that are missing. Idempotent:
     * existsByStartTime skips ones already there, and start_time's UNIQUE
     * constraint is the backstop if two requests race.
     */
    @Transactional
    public void ensureDayExists(LocalDate date) {
        for (LocalTime t = OPEN; t.isBefore(CLOSE); t = t.plusMinutes(SLOT_MINUTES)) {
            OffsetDateTime start = date.atTime(t).atZone(ZONE).toOffsetDateTime();
            if (!slotRepository.existsByStartTime(start)) {
                slotRepository.save(new Slot(start, DEFAULT_CAPACITY));
            }
        }
    }

    /** The slots of a single day, in time order. */
    @Transactional(readOnly = true)
    public List<Slot> listDay(LocalDate date) {
        OffsetDateTime from = date.atTime(OPEN).atZone(ZONE).toOffsetDateTime();
        OffsetDateTime toExclusive = date.atTime(CLOSE).atZone(ZONE).toOffsetDateTime();
        return slotRepository
                .findByStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTime(from, toExclusive);
    }
}
