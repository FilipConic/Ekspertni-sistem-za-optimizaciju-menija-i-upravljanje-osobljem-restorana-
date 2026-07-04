package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {
    public enum Type {
        REMOVE_ITEM,
        CHANGE_PRICE,
        START_PROMOTION,
        ADD_NEW_ITEM,
        WAITER_WARNING,
        ADDITIONAL_TRAINING,
        WAITER_REWARD,
        RESCHEDULE_WAITER,
        SURGE_PRICING,
    }

    private Type type;
    private String subject;
    private String explanation;
}
