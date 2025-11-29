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
public class Cafe24OrderDetailResponse {
    
    @JsonProperty("order")
    private OrderDetail order;

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderDetail {
        @JsonProperty("order_id")
        private String orderId;

        @JsonProperty("items")
        private List<OrderItemDetail> items;

        // 알 수 없는 필드도 받을 수 있도록 Map 추가
        @JsonProperty
        private Map<String, Object> additionalProperties;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OrderItemDetail {
        @JsonProperty("item_code")
        private String itemCode;

        @JsonProperty("product_code")
        private String productCode;

        @JsonProperty("product_name")
        private String productName;

        @JsonProperty("quantity")
        private Integer quantity;

        @JsonProperty("price")
        private Object price;

        @JsonProperty("option_value")
        private String optionValue;

        @JsonProperty("option_code")
        private String optionCode;

        @JsonProperty("variant_code")
        private String variantCode;

        // 알 수 없는 필드도 받을 수 있도록 Map 추가
        @JsonProperty
        private Map<String, Object> additionalProperties;
    }
}

