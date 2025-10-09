package com.example.E_shopping.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class OrderIdGenerator {

    public static String generateOrderId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // e.g., 20251009
        String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase(); // e.g., HK3D2
        return "ORD-" + datePart + "-" + randomPart;
    }

    public static String generatePaymentId() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String randomPart = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        return "PAY-" + datePart + "-" + randomPart;
    }
}
