package com.almang.inventory.global.cafe24.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class Cafe24OrderResponse {
    @JsonProperty("orders")
    private List<Order> orders;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Order {
        @JsonProperty("order_id")
        private String orderId;

        @JsonProperty("order_date")
        private String orderDate;

        @JsonProperty("items")
        private List<OrderItem> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class OrderItem {
        @JsonProperty("product_code")
        private String productCode;

        @JsonProperty("quantity")
        private Integer quantity;
    }
}
