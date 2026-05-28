package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    private int tableNumber;
    private String waiterName;
    private List<String> menuItems;
    private LocalDateTime createdAt;
    private double totalAmount;
    private boolean cancelled;
}
