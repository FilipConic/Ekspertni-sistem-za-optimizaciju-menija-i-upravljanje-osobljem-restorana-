package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Waiter {
    private String name;
    private double avgTablesPerShift;
    private double avgOrderValue;
    private double guestRating;
}
