package com.almang.inventory.global.cafe24.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Cafe24OrderResponse {
    @JsonProperty("orders")
    private List<Order> orders;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Order {
        // 주문 기본 정보
        @JsonProperty("order_id")
        private String orderId;

        @JsonProperty("order_date")
        private String orderDate;

        @JsonProperty("paid")
        private String paid; // "T" 또는 "F"

        @JsonProperty("canceled")
        private String canceled; // "T" 또는 "F"

        @JsonProperty("payment_method")
        private List<String> paymentMethod; // 배열

        @JsonProperty("payment_method_name")
        private List<String> paymentMethodName; // 배열

        @JsonProperty("payment_amount")
        private String paymentAmount;

        // 주문자 정보
        @JsonProperty("billing_name")
        private String billingName; // 주문자 이름

        @JsonProperty("member_id")
        private String memberId;

        @JsonProperty("member_email")
        private String memberEmail;

        // 주문 금액 정보
        @JsonProperty("initial_order_amount")
        private OrderAmount initialOrderAmount;

        @JsonProperty("actual_order_amount")
        private OrderAmount actualOrderAmount;

        // 주문 상품 목록 (실제로는 별도 API 호출 필요할 수 있음)
        @JsonProperty("items")
        private List<OrderItem> items;

        // 알 수 없는 필드도 받을 수 있도록 Map 추가
        @JsonProperty
        private Map<String, Object> additionalProperties;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderAmount {
        @JsonProperty("order_price_amount")
        private String orderPriceAmount;

        @JsonProperty("shipping_fee")
        private String shippingFee;

        @JsonProperty("total_amount_due")
        private String totalAmountDue;

        @JsonProperty("payment_amount")
        private String paymentAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItem {
        // 상품 기본 정보
        @JsonProperty("product_code")
        private String productCode;

        @JsonProperty("product_name")
        private String productName;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("price")
        private Object price; // String 또는 Number일 수 있음

        @JsonProperty("option_value")
        private String optionValue;

        @JsonProperty("option_code")
        private String optionCode;

        @JsonProperty("variant_code")
        private String variantCode;

        @JsonProperty("item_code")
        private String itemCode;

        // 알 수 없는 필드도 받을 수 있도록 Map 추가
        @JsonProperty
        private Map<String, Object> additionalProperties;
    }
}
