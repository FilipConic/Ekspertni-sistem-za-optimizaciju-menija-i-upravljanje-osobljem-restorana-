package com.restaurant.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaiterFact {
    private String waiterName;
    private boolean problematic;
    private boolean excellent;
    private boolean consistentlyBad;
    private boolean consistentlyGood;
}
