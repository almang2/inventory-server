package com.almang.inventory.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.order.domain.OrderStatus;
import com.almang.inventory.order.dto.request.CreateOrderItemRequest;
import com.almang.inventory.order.dto.request.CreateOrderRequest;
import com.almang.inventory.order.dto.request.UpdateOrderItemRequest;
import com.almang.inventory.order.dto.request.UpdateOrderRequest;
import com.almang.inventory.order.dto.response.OrderResponse;
import com.almang.inventory.order.service.OrderService;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrderService orderService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UsernamePasswordAuthenticationToken auth() {
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "store_admin", List.of());
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    @Test
    void 발주_생성에_성공한다() throws Exception {
        // given
        CreateOrderItemRequest item1 = new CreateOrderItemRequest(1L, 10, 1000, "비고1");
        CreateOrderItemRequest item2 = new CreateOrderItemRequest(2L, 5, 2000, "비고2");

        CreateOrderRequest request = new CreateOrderRequest(
                10L,
                "발주 메시지",
                3,
                List.of(item1, item2)
        );

        OrderResponse response = new OrderResponse(
                100L,
                10L,
                10L,
                "메시지입니다.",
                OrderStatus.REQUEST,
                3,
                LocalDate.now().plusDays(3),
                null,
                null,
                true,
                50000,
                List.of()
        );

        when(orderService.createOrder(any(CreateOrderRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/order")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.CREATE_ORDER_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.orderId").value(100L))
                .andExpect(jsonPath("$.data.vendorId").value(10L))
                .andExpect(jsonPath("$.data.totalPrice").value(50000))
                .andExpect(jsonPath("$.data.orderMessage").value("메시지입니다."))
                .andExpect(jsonPath("$.data.leadTime").value(3))
                .andExpect(jsonPath("$.data.activated").value(true));
    }

    @Test
    void 발주_생성_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                10L,
                "발주 메시지",
                3,
                List.of(new CreateOrderItemRequest(1L, 10, 1000, "비고"))
        );

        when(orderService.createOrder(any(CreateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/order")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_생성_시_발주처가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                999L,
                "메시지",
                2,
                List.of(new CreateOrderItemRequest(1L, 3, 1000, "비고"))
        );

        when(orderService.createOrder(any(CreateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/order")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.VENDOR_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.VENDOR_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_생성_시_발주처_접근_권한이_없으면_예외가_발생한다() throws Exception {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                10L,
                "메시지",
                3,
                List.of(new CreateOrderItemRequest(1L, 3, 1000, "비고"))
        );

        when(orderService.createOrder(any(CreateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_ACCESS_DENIED));

        // when & then
        mockMvc.perform(post("/api/v1/order")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.VENDOR_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.VENDOR_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_생성_시_상품이_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                10L,
                "메시지",
                3,
                List.of(new CreateOrderItemRequest(9999L, 3, 1000, "비고"))
        );

        when(orderService.createOrder(any(CreateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/order")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.PRODUCT_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PRODUCT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_생성_시_상품_접근_권한이_없으면_예외가_발생한다() throws Exception {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                10L,
                "메시지",
                3,
                List.of(new CreateOrderItemRequest(1L, 5, 1000, "비고"))
        );

        when(orderService.createOrder(any(CreateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.PRODUCT_ACCESS_DENIED));

        // when & then
        mockMvc.perform(post("/api/v1/order")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.PRODUCT_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PRODUCT_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_생성_시_주문_항목이_비어있으면_예외가_발생한다() throws Exception {
        // given
        CreateOrderRequest request = new CreateOrderRequest(
                10L,
                "메시지",
                3,
                List.of()
        );

        when(orderService.createOrder(any(CreateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.INVALID_INPUT_VALUE));

        // when & then
        mockMvc.perform(post("/api/v1/order")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_조회에_성공한다() throws Exception {
        // given
        Long orderId = 100L;

        OrderResponse response = new OrderResponse(
                orderId,
                10L,
                10L,
                "조회 메시지 입니다",
                OrderStatus.REQUEST,
                3,
                LocalDate.now().plusDays(3),
                null,
                null,
                true,
                30000,
                List.of()
        );

        when(orderService.getOrder(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/order/{orderId}", orderId)
                                .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.GET_ORDER_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.vendorId").value(10L))
                .andExpect(jsonPath("$.data.totalPrice").value(30000))
                .andExpect(jsonPath("$.data.activated").value(true));
    }

    @Test
    void 발주_조회시_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long notExistId = 9999L;

        when(orderService.getOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/order/{orderId}", notExistId)
                                .with(authentication(auth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.ORDER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_조회시_다른_상점의_발주면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        when(orderService.getOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/v1/order/{orderId}", orderId)
                        .with(authentication(auth())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.ORDER_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_목록_조회에_성공한다() throws Exception {
        // given
        OrderResponse r1 = new OrderResponse(
                1L,
                10L,
                10L,
                "메시지1",
                OrderStatus.REQUEST,
                1,
                LocalDate.now().plusDays(1),
                null,
                null,
                true,
                5000,
                List.of()
        );

        OrderResponse r2 = new OrderResponse(
                2L,
                10L,
                10L,
                "메시지2",
                OrderStatus.REQUEST,
                2,
                LocalDate.now().plusDays(2),
                null,
                null,
                true,
                9000,
                List.of()
        );

        PageResponse<OrderResponse> pageResponse = new PageResponse<>(
                List.of(r1, r2),
                1,
                20,
                2L,
                1,
                true
        );

        when(orderService.getOrderList(anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/v1/order")
                        .param("page", "1")
                        .param("size", "20")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_ORDER_LIST_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].orderId").value(1))
                .andExpect(jsonPath("$.data.content[1].orderId").value(2));
    }

    @Test
    void 발주_목록_조회시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        when(orderService.getOrderList(anyLong(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/order")
                        .param("page", "1")
                        .param("size", "20")
                        .with(authentication(auth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_목록_조회시_상점_접근_권한이_없으면_예외가_발생한다() throws Exception {
        // given
        when(orderService.getOrderList(anyLong(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BaseException(ErrorCode.ORDER_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/v1/order")
                        .param("page", "1")
                        .param("size", "20")
                        .with(authentication(auth())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.ORDER_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorCode.ORDER_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_수정에_성공한다() throws Exception {
        // given
        Long orderId = 100L;

        UpdateOrderItemRequest itemReq = new UpdateOrderItemRequest(
                1L,
                1L,
                20,
                1500,
                "수정된 비고"
        );

        UpdateOrderRequest request = new UpdateOrderRequest(
                10L,
                OrderStatus.IN_PRODUCTION,
                "수정된 메시지",
                5,
                LocalDate.now(),
                null,
                false,
                List.of(itemReq)
        );

        OrderResponse response = new OrderResponse(
                orderId,
                10L,
                10L,
                "수정된 메시지",
                OrderStatus.IN_PRODUCTION,
                5,
                LocalDate.now().plusDays(5),
                LocalDate.now(),
                null,
                false,
                30000,
                List.of()
        );

        when(orderService.updateOrder(anyLong(), any(UpdateOrderRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.UPDATE_ORDER_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.vendorId").value(10L))
                .andExpect(jsonPath("$.data.activated").value(false));
    }

    @Test
    void 발주_수정시_발주가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long notExistOrderId = 9999L;

        UpdateOrderRequest request = new UpdateOrderRequest(
                10L,
                OrderStatus.IN_PRODUCTION,
                "수정 메시지",
                5,
                LocalDate.now(),
                null,
                true,
                List.of()
        );

        when(orderService.updateOrder(anyLong(), any(UpdateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/order/{orderId}", notExistOrderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.ORDER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_수정시_다른_상점의_발주면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        UpdateOrderRequest request = new UpdateOrderRequest(
                10L,
                OrderStatus.IN_PRODUCTION,
                "수정 메시지",
                5,
                LocalDate.now(),
                null,
                true,
                List.of()
        );

        when(orderService.updateOrder(anyLong(), any(UpdateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_ACCESS_DENIED));

        // when & then
        mockMvc.perform(patch("/api/v1/order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.ORDER_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_수정시_발주처를_변경하려_하면_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        UpdateOrderRequest request = new UpdateOrderRequest(
                999L,
                OrderStatus.IN_PRODUCTION,
                "수정 메시지",
                5,
                LocalDate.now(),
                null,
                true,
                List.of()
        );

        when(orderService.updateOrder(anyLong(), any(UpdateOrderRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_CHANGE_NOT_ALLOWED));

        // when & then
        mockMvc.perform(patch("/api/v1/order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.VENDOR_CHANGE_NOT_ALLOWED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorCode.VENDOR_CHANGE_NOT_ALLOWED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
