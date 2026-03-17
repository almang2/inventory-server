package com.almang.inventory.retail.dto.excel;

public record SkippedRow(
        int rowIndex,
        String code,
        SkipReason reason,
        String message
) {
    public static SkippedRow of(int rowIndex, String code, SkipReason reason) {
        return new SkippedRow(rowIndex, code, reason, reason.defaultMessage());
    }

    public static SkippedRow of(int rowIndex, String code, SkipReason reason, String message) {
        return new SkippedRow(rowIndex, code, reason, message);
    }
}
