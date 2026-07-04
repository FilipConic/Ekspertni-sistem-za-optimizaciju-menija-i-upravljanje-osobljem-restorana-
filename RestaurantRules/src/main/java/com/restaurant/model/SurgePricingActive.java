package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Marker fact: surge pricing is currently active for a shift.
 * Prevents the CEP rule from re-firing every cycle while the ratio
 * stays high, and gets retracted once the ratio normalizes.
 */
@Data
@AllArgsConstructor
public class SurgePricingActive {
    private int shiftId;
}
