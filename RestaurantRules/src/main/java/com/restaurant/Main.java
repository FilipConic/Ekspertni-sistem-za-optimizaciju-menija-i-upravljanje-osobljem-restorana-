package com.restaurant;

import com.restaurant.model.*;
import com.restaurant.service.DroolsService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        DroolsService service = new DroolsService();

        List<MenuItem> menuItems = Arrays.asList(
                new MenuItem("Riblja corba", "Predjelo", 500, 280, 12),
                new MenuItem("Biftek", "Glavno jelo", 2200, 900, 45),
                new MenuItem("Tiramisu", "Desert", 700, 150, 8)
        );

        List<Waiter> waiters = Arrays.asList(
                new Waiter("Ana", 9.5, 3200, 4.8),
                new Waiter("Marko", 3.5, 1800, 2.4)
        );

        List<Shift> shifts = Arrays.asList(
                new Shift(LocalDate.now().minusDays(1), Arrays.asList("Ana"), 40, 28000),
                new Shift(LocalDate.now().minusDays(2), Arrays.asList("Marko"), 15, 4500)
        );

        List<Recommendation> recommendations = service.runForwardChaining(menuItems, waiters, shifts);

        System.out.println("Preporuke:");
        for (Recommendation rec : recommendations) {
            System.out.println(rec.getType() + " - " + rec.getSubject() + ": " + rec.getExplanation());
        }
    }
}
