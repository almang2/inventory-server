package com.almang.inventory.customerorder.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerOrderRequest {

    @NotBlank
    @JsonProperty("order_id")
    private String cafe24OrderId;

    @NotNull
    @JsonProperty("order_date")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "Asia/Seoul") // 이 라인을 추가합니다.
    private LocalDateTime orderAt;

    @NotNull
    @JsonProperty("paid") // 'T' 또는 'F' 문자열이 들어올 수 있으므로, 실제 파싱 시점에 boolean으로 변환 필요
    private String isPaid; // 일단 문자열로 받아서 파싱 로직에서 처리

    @NotNull
    @JsonProperty("canceled")
    private String isCanceled; // 일단 문자열로 받아서 파싱 로직에서 처리

    @JsonProperty("payment_method_name")
    private List<String> paymentMethodName; // 리스트 형태로 들어옴

    @NotNull
    @JsonProperty("payment_amount")
    private BigDecimal paymentAmount;

    @NotBlank
    @JsonProperty("billing_name")
    private String billingName;

    @JsonProperty("member_id")
    private String memberId;

    @JsonProperty("member_email")
    private String memberEmail;

    @NotNull
    @JsonProperty("initial_order_amount")
    private InitialOrderAmount initialOrderAmount;

    @Valid
    @JsonProperty("items") // 주문 상품 목록
    private List<CustomerOrderItemRequest> items;

    // 카페24 JSON 구조에 맞는 내부 클래스 정의
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InitialOrderAmount {
        @NotNull
        @JsonProperty("order_price_amount")
        private BigDecimal orderPriceAmount;

        @NotNull
        @JsonProperty("shipping_fee")
        private BigDecimal shippingFee;
    }
}
