package com.almang.inventory.inventory.domain;

public enum InventoryScope {
    ALL,
    DISPLAY,
    WAREHOUSE,
    OUTGOING,
    INCOMING;

    public static InventoryScope from(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }

        try {
            return InventoryScope.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ALL;
        }
    }
}
