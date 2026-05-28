package com.restaurant.util;

import java.time.LocalDate;

public class DateHelper {
    public static LocalDate twoWeeksAgo() {
        return LocalDate.now().minusDays(14);
    }
}
