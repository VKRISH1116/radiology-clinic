package com.clinic.booking;

import java.time.OffsetDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Business rule: how close to an appointment a patient may still self-cancel or
 * reschedule it. Inside the cutoff window they must contact the clinic instead.
 *
 * The window is configurable (app.booking.change-cutoff-hours, default 2). The
 * decision logic {@link #allowsChangeAt} is pure so the boundary is unit-testable
 * without wall-clock flakiness.
 */
@Component
public class ChangeCutoffPolicy {

    private final long cutoffHours;

    public ChangeCutoffPolicy(@Value("${app.booking.change-cutoff-hours}") long cutoffHours) {
        this.cutoffHours = cutoffHours;
    }

    /** True if a slot starting at {@code slotStart} may still be changed at {@code now}. */
    public boolean allowsChangeAt(OffsetDateTime slotStart, OffsetDateTime now) {
        return !slotStart.isBefore(now.plusHours(cutoffHours));
    }

    /** Enforce the rule against the current time, throwing 409 if inside the window. */
    public void check(OffsetDateTime slotStart) {
        if (!allowsChangeAt(slotStart, OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Too close to the appointment to change it (must be at least "
                            + cutoffHours + "h before the start time)");
        }
    }
}
