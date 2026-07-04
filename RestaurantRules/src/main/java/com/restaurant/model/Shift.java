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
    private int id;
    private LocalDate date;
    private List<Integer> waiterIds;
    private int guestCount;
    private double totalEarnings;
}
