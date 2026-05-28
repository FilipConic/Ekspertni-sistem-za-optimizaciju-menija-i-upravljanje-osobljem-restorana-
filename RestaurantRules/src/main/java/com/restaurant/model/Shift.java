package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Shift {
    private LocalDate date;
    private List<String> waiters;
    private int guestCount;
    private double totalEarnings;
}
