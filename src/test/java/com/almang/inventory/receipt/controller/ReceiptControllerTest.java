package com.almang.inventory.receipt.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.receipt.domain.ReceiptStatus;
import com.almang.inventory.receipt.dto.response.ReceiptItemResponse;
import com.almang.inventory.receipt.dto.response.ReceiptResponse;
import com.almang.inventory.receipt.service.ReceiptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

@WebMvcTest(ReceiptController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class ReceiptControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ReceiptService receiptService;
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
    void 발주기반_입고_생성에_성공한다() throws Exception {
        // given
        Long orderId = 100L;

        ReceiptItemResponse item = new ReceiptItemResponse(
                1000L,
                1L,
                10L,
                null,
                null,
                BigDecimal.valueOf(5),
                null,
                5000,
                null,
                "비고입니다."
        );

        ReceiptResponse response = new ReceiptResponse(
                1L,
                10L,
                orderId,
                LocalDate.now(),
                0,
                null,
                ReceiptStatus.PENDING,
                true,
                List.of(item)
        );

        when(receiptService.createReceiptFromOrder(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/receipt/from-order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.CREATE_RECEIPT_FROM_ORDER_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.receiptId").value(1L))
                .andExpect(jsonPath("$.data.storeId").value(10L))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.status").value(ReceiptStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.activated").value(true))
                .andExpect(jsonPath("$.data.receiptItems[0].receiptItemId").value(1000L))
                .andExpect(jsonPath("$.data.receiptItems[0].productId").value(10L))
                .andExpect(jsonPath("$.data.receiptItems[0].amount").value(5000));
    }

    @Test
    void 발주기반_입고_생성시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        when(receiptService.createReceiptFromOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/receipt/from-order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주기반_입고_생성시_발주가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long notExistOrderId = 9999L;

        when(receiptService.createReceiptFromOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/receipt/from-order/{orderId}", notExistOrderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.ORDER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주기반_입고_생성시_다른_상점의_발주면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        when(receiptService.createReceiptFromOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_ACCESS_DENIED));

        // when & then
        mockMvc.perform(post("/api/v1/receipt/from-order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.ORDER_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주기반_입고_생성시_취소된_발주면_입고_생성이_불가하다는_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        when(receiptService.createReceiptFromOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_CREATION_NOT_ALLOWED_FROM_ORDER));

        // when & then
        mockMvc.perform(post("/api/v1/receipt/from-order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_CREATION_NOT_ALLOWED_FROM_ORDER.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_CREATION_NOT_ALLOWED_FROM_ORDER.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주기반_입고_조회에_성공한다() throws Exception {
        // given
        Long orderId = 100L;

        ReceiptItemResponse item = new ReceiptItemResponse(
                1000L,
                1L,
                10L,
                null,
                null,
                BigDecimal.valueOf(5),
                null,
                5000,
                null,
                "비고입니다."
        );

        ReceiptResponse response = new ReceiptResponse(
                1L,
                10L,
                orderId,
                LocalDate.now(),
                0,
                null,
                ReceiptStatus.PENDING,
                true,
                List.of(item)
        );

        when(receiptService.getReceiptFromOrder(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/receipt/from-order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.GET_RECEIPT_FROM_ORDER_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.receiptId").value(1L))
                .andExpect(jsonPath("$.data.storeId").value(10L))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.status").value(ReceiptStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.activated").value(true))
                .andExpect(jsonPath("$.data.receiptItems[0].receiptItemId").value(1000L))
                .andExpect(jsonPath("$.data.receiptItems[0].productId").value(10L))
                .andExpect(jsonPath("$.data.receiptItems[0].amount").value(5000));
    }

    @Test
    void 발주기반_입고_조회시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        when(receiptService.getReceiptFromOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/from-order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주기반_입고_조회시_발주가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long notExistOrderId = 9999L;

        when(receiptService.getReceiptFromOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/from-order/{orderId}", notExistOrderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.ORDER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주기반_입고_조회시_다른_상점의_발주면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        when(receiptService.getReceiptFromOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/from-order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.ORDER_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.ORDER_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주기반_입고_조회시_입고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long orderId = 100L;

        when(receiptService.getReceiptFromOrder(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/from-order/{orderId}", orderId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
