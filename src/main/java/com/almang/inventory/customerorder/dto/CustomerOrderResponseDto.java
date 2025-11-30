package com.almang.inventory.customerorder.dto;

import com.almang.inventory.customerorder.domain.CustomerOrder;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class CustomerOrderResponseDto {
    private Long id;
    private String cafe24OrderId;
    private LocalDateTime orderAt;
    private boolean isPaid;
    private boolean isCanceled;
    private String paymentMethod;
    private BigDecimal paymentAmount;
    private String billingName;
    private String memberId;
    private String memberEmail;
    private BigDecimal initialOrderPriceAmount;
    private BigDecimal shippingFee;
    private List<CustomerOrderItemResponseDto> items;

    public static CustomerOrderResponseDto from(CustomerOrder order) {
        return CustomerOrderResponseDto.builder()
                .id(order.getId())
                .cafe24OrderId(order.getCafe24OrderId())
                .orderAt(order.getOrderAt())
                .isPaid(order.isPaid())
                .isCanceled(order.isCanceled())
                .paymentMethod(order.getPaymentMethod())
                .paymentAmount(order.getPaymentAmount())
                .billingName(order.getBillingName())
                .memberId(order.getMemberId())
                .memberEmail(order.getMemberEmail())
                .initialOrderPriceAmount(order.getInitialOrderPriceAmount())
                .shippingFee(order.getShippingFee())
                .items(order.getItems().stream()
                        .map(CustomerOrderItemResponseDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
}
