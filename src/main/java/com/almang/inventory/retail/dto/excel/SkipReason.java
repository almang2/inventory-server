package com.almang.inventory.retail.dto.excel;

public enum SkipReason {
    PRODUCT_NOT_FOUND("상품을 찾을 수 없어 스킵"),
    INVENTORY_NOT_FOUND("재고 레코드가 없어 스킵"),
    INSUFFICIENT_STOCK("재고 부족으로 스킵");

    private final String defaultMessage;

    SkipReason(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
