package com.almang.inventory.cafe24.service;

import com.almang.inventory.customerorder.domain.CustomerOrder;
import com.almang.inventory.customerorder.service.CustomerOrderService;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.vendor.repository.VendorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class Cafe24OrderServiceTest {

    private Cafe24OrderService cafe24OrderService;

    @Mock
    private CustomerOrderService customerOrderService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private com.almang.inventory.store.repository.StoreRepository storeRepository;

    @Mock
    private com.almang.inventory.vendor.repository.VendorRepository vendorRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        cafe24OrderService = new Cafe24OrderService(
                productRepository,
                storeRepository,
                vendorRepository,
                customerOrderService,
                objectMapper);
    }

    @Test
    @DisplayName("N00(입금전) 주문 처리 시 CustomerOrderService.registerNewOrder가 호출되어야 한다")
    void processBeforeDepositOrders_ShouldCallRegisterNewOrder() {
        // given
        String jsonResponse = "{\"orders\": [{\"order_id\": \"20231201-0000001\", \"order_date\": \"2023-12-01T10:00:00+09:00\", \"items\": []}]}";

        given(customerOrderService.findByCafe24OrderId(anyString())).willReturn(Optional.empty());
        given(customerOrderService.registerNewOrder(any(CustomerOrder.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        cafe24OrderService.processBeforeDepositOrders(jsonResponse);

        // then
        verify(customerOrderService, times(1)).registerNewOrder(any(CustomerOrder.class));
    }

    @Test
    @DisplayName("N10(배송준비) 주문 처리 시 기존 주문이 있으면 processPaymentCompletion이 호출되어야 한다")
    void processPreparingShipmentOrders_ShouldCallProcessPaymentCompletion_WhenOrderExists() {
        // given
        String jsonResponse = "{\"orders\": [{\"order_id\": \"20231201-0000001\", \"order_date\": \"2023-12-01T10:00:00+09:00\", \"items\": []}]}";
        CustomerOrder existingOrder = CustomerOrder.builder()
                .cafe24OrderId("20231201-0000001")
                .isPaid(false)
                .build();

        given(customerOrderService.findByCafe24OrderId("20231201-0000001")).willReturn(Optional.of(existingOrder));

        // when
        cafe24OrderService.processPreparingShipmentOrders(jsonResponse);

        // then
        verify(customerOrderService, times(1)).processPaymentCompletion(existingOrder, true);
    }
}
