package com.restaurant.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * A CEP event: a point-in-time reading of how many guests are currently
 * being served on a shift vs how many waiters are actively working it.
 * shiftId here is just a correlation key for the event stream (it doesn't
 * have to reference a persisted Shift row) - kept as int to match Shift.id.
 */
@Data
public class StaffingSnapshotEvent {
    private int shiftId;
    private int guestCount;
    private int activeWaiterCount;
    private LocalDateTime timestamp;

    public StaffingSnapshotEvent(int shiftId, int guestCount, int activeWaiterCount) {
        this.shiftId = shiftId;
        this.guestCount = guestCount;
        this.activeWaiterCount = activeWaiterCount;
        this.timestamp = LocalDateTime.now();
    }

    public double getRatio() {
        if (activeWaiterCount <= 0) return Double.MAX_VALUE;
        return (double) guestCount / (double) activeWaiterCount;
    }

    @Override
    public String toString() {
        return "StaffingSnapshotEvent{shift=" + shiftId + ", guests=" + guestCount
                + ", waiters=" + activeWaiterCount + ", ratio=" + String.format("%.2f", getRatio()) + "}";
    }
}
