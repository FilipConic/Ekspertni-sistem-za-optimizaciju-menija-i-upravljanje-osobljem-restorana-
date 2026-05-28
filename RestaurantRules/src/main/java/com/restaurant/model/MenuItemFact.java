package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemFact {
    private String menuItemName;
    private double profitMargin;
    private boolean bestseller;
    private boolean unprofitable;
    private boolean highPotential;
    private boolean priceProblem;
}
