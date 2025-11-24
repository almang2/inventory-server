package com.almang.inventory.receipt.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.receipt.domain.ReceiptStatus;
import com.almang.inventory.receipt.dto.request.UpdateReceiptItemRequest;
import com.almang.inventory.receipt.dto.request.UpdateReceiptRequest;
import com.almang.inventory.receipt.dto.response.ConfirmReceiptResponse;
import com.almang.inventory.receipt.dto.response.DeleteReceiptItemResponse;
import com.almang.inventory.receipt.dto.response.DeleteReceiptResponse;
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

        ReceiptItemResponse item =
                new ReceiptItemResponse(
                1000L,
                1L,
                10L,
                null,
                null,
                BigDecimal.valueOf(5),
                null,
                5000,
                null,
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
                .andExpect(jsonPath("$.data.receiptItems[0].productId").value(10L));
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
                .andExpect(jsonPath("$.data.receiptItems[0].productId").value(10L));
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

    @Test
    void 입고_조회에_성공한다() throws Exception {
        // given
        Long receiptId = 1L;

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
                null,
                "비고입니다."
        );

        ReceiptResponse response = new ReceiptResponse(
                receiptId,
                10L,
                200L,
                LocalDate.now(),
                0,
                null,
                ReceiptStatus.PENDING,
                true,
                List.of(item)
        );

        when(receiptService.getReceipt(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.GET_RECEIPT_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.receiptId").value(receiptId))
                .andExpect(jsonPath("$.data.storeId").value(10L))
                .andExpect(jsonPath("$.data.orderId").value(200L))
                .andExpect(jsonPath("$.data.status").value(ReceiptStatus.PENDING.name()))
                .andExpect(jsonPath("$.data.activated").value(true))
                .andExpect(jsonPath("$.data.receiptItems[0].receiptItemId").value(1000L))
                .andExpect(jsonPath("$.data.receiptItems[0].productId").value(10L));
    }

    @Test
    void 입고_조회시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 1L;

        when(receiptService.getReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_조회시_입고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 9999L;

        when(receiptService.getReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_조회시_다른_상점의_입고면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 123L;

        when(receiptService.getReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_목록_조회에_성공한다() throws Exception {
        // given
        ReceiptItemResponse item1 = new ReceiptItemResponse(
                1001L, 1L, 101L,
                null, null,
                BigDecimal.valueOf(5),
                null, 5000, null,
                null, "비고1"
        );

        ReceiptItemResponse item2 = new ReceiptItemResponse(
                1002L, 1L, 102L,
                null, null,
                BigDecimal.valueOf(3),
                null, 3000, null,
                null, "비고2"
        );

        ReceiptResponse r1 = new ReceiptResponse(
                1L, 10L, 100L,
                LocalDate.now(),
                0,
                null,
                ReceiptStatus.PENDING,
                true,
                List.of(item1)
        );

        ReceiptResponse r2 = new ReceiptResponse(
                2L, 10L, 101L,
                LocalDate.now(),
                0,
                null,
                ReceiptStatus.PENDING,
                true,
                List.of(item2)
        );

        PageResponse<ReceiptResponse> pageResponse = new PageResponse<>(
                List.of(r1, r2),
                1,
                20,
                2L,
                1,
                true
        );

        when(receiptService.getReceiptList(anyLong(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/v1/receipt")
                        .param("page", "1")
                        .param("size", "20")
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.GET_RECEIPT_LIST_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].receiptId").value(1))
                .andExpect(jsonPath("$.data.content[1].receiptId").value(2));
    }

    @Test
    void 입고_목록_조회시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        when(receiptService.getReceiptList(anyLong(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/receipt")
                        .param("page", "1")
                        .param("size", "20")
                        .with(authentication(auth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_목록_조회시_상점_접근_권한이_없으면_예외가_발생한다() throws Exception {
        // given
        when(receiptService.getReceiptList(anyLong(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/v1/receipt")
                        .param("page", "1")
                        .param("size", "20")
                        .with(authentication(auth())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_수정에_성공한다() throws Exception {
        // given
        Long receiptId = 1L;
        Long orderId = 200L;

        UpdateReceiptItemRequest updateItem = new UpdateReceiptItemRequest(
                1000L,
                receiptId,
                2,
                BigDecimal.valueOf(1.234),
                null,
                10,
                1100,
                "수정 비고입니다."
        );

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                orderId,
                null,
                BigDecimal.valueOf(10.000),
                ReceiptStatus.CONFIRMED,
                true,
                List.of(updateItem)
        );

        ReceiptItemResponse itemResponse = new ReceiptItemResponse(
                1000L,
                1L,
                10L,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                11000,
                110000,
                BigDecimal.valueOf(1.000),
                "수정 비고입니다."
        );

        ReceiptResponse response = new ReceiptResponse(
                receiptId,
                10L,
                orderId,
                LocalDate.now(),
                2,
                BigDecimal.valueOf(10.000),
                ReceiptStatus.CONFIRMED,
                true,
                List.of(itemResponse)
        );

        when(receiptService.updateReceipt(anyLong(), any(UpdateReceiptRequest.class), anyLong()))
                .thenReturn(response);

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.UPDATE_RECEIPT_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.receiptId").value(receiptId))
                .andExpect(jsonPath("$.data.storeId").value(10L))
                .andExpect(jsonPath("$.data.orderId").value(orderId))
                .andExpect(jsonPath("$.data.status").value(ReceiptStatus.CONFIRMED.name()))
                .andExpect(jsonPath("$.data.activated").value(true))
                .andExpect(jsonPath("$.data.totalBoxCount").value(2))
                .andExpect(jsonPath("$.data.receiptItems[0].receiptItemId").value(1000L))
                .andExpect(jsonPath("$.data.receiptItems[0].productId").value(10L))
                .andExpect(jsonPath("$.data.receiptItems[0].boxCount").value(2))
                .andExpect(jsonPath("$.data.receiptItems[0].amount").value(110000));
    }

    @Test
    void 입고_수정시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 1L;

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                200L,
                null,
                null,
                null,
                null,
                List.of()
        );

        when(receiptService.updateReceipt(anyLong(), any(UpdateReceiptRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_수정시_입고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 9999L;

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                200L,
                null,
                null,
                null,
                null,
                List.of()
        );

        when(receiptService.updateReceipt(anyLong(), any(UpdateReceiptRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_NOT_FOUND));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_수정시_발주ID가_변경되면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 1L;

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                999L,
                null,
                null,
                null,
                null,
                List.of()
        );

        when(receiptService.updateReceipt(anyLong(), any(UpdateReceiptRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ORDER_MISMATCH));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ORDER_MISMATCH.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ORDER_MISMATCH.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_수정시_다른_입고의_아이템이면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 1L;

        UpdateReceiptItemRequest wrongItem = new UpdateReceiptItemRequest(
                9999L,
                receiptId,
                1,
                null,
                null,
                5,
                1000,
                "잘못된 아이템"
        );

        UpdateReceiptRequest request = new UpdateReceiptRequest(
                200L,
                null,
                null,
                null,
                null,
                List.of(wrongItem)
        );

        when(receiptService.updateReceipt(anyLong(), any(UpdateReceiptRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_삭제에_성공한다() throws Exception {
        // given
        Long receiptId = 1L;

        when(receiptService.deleteReceipt(anyLong(), anyLong()))
                .thenReturn(new DeleteReceiptResponse(true));

        // when & then
        mockMvc.perform(delete("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.DELETE_RECEIPT_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void 입고_삭제시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 1L;

        when(receiptService.deleteReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(delete("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_삭제시_입고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 9999L;

        when(receiptService.deleteReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_NOT_FOUND));

        // when & then
        mockMvc.perform(delete("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_삭제시_다른_상점의_입고면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 123L;

        when(receiptService.deleteReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ACCESS_DENIED));

        // when & then
        mockMvc.perform(delete("/api/v1/receipt/{receiptId}", receiptId)
                        .with(authentication(auth())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_조회에_성공한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        ReceiptItemResponse response = new ReceiptItemResponse(
                receiptItemId,
                1L,
                10L,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                5000,
                50000,
                BigDecimal.valueOf(1.000),
                "비고입니다."
        );

        when(receiptService.getReceiptItem(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.GET_RECEIPT_ITEM_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.receiptItemId").value(receiptItemId))
                .andExpect(jsonPath("$.data.receiptId").value(1L))
                .andExpect(jsonPath("$.data.productId").value(10L))
                .andExpect(jsonPath("$.data.boxCount").value(2))
                .andExpect(jsonPath("$.data.amount").value(50000));
    }

    @Test
    void 입고_아이템_조회시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        when(receiptService.getReceiptItem(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_조회시_아이템이_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long notExistReceiptItemId = 9999L;

        when(receiptService.getReceiptItem(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ITEM_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/receipt/{receiptItemId}", notExistReceiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ITEM_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ITEM_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_조회시_다른_상점의_아이템이면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        when(receiptService.getReceiptItem(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_수정에_성공한다() throws Exception {
        // given
        Long receiptItemId = 1000L;
        Long receiptId = 1L;

        UpdateReceiptItemRequest request = new UpdateReceiptItemRequest(
                receiptItemId,
                receiptId,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                1500,
                "수정 비고입니다."
        );

        ReceiptItemResponse response = new ReceiptItemResponse(
                receiptItemId,
                receiptId,
                10L,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                1500,
                15000,
                BigDecimal.valueOf(0.0),
                "수정 비고입니다."
        );

        when(receiptService.updateReceiptItem(anyLong(), any(UpdateReceiptItemRequest.class), anyLong()))
                .thenReturn(response);

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.UPDATE_RECEIPT_ITEM_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.receiptItemId").value(receiptItemId))
                .andExpect(jsonPath("$.data.receiptId").value(receiptId))
                .andExpect(jsonPath("$.data.productId").value(10L))
                .andExpect(jsonPath("$.data.boxCount").value(2))
                .andExpect(jsonPath("$.data.actualQuantity").value(10))
                .andExpect(jsonPath("$.data.unitPrice").value(1500))
                .andExpect(jsonPath("$.data.amount").value(15000));
    }

    @Test
    void 입고_아이템_수정시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        UpdateReceiptItemRequest request = new UpdateReceiptItemRequest(
                receiptItemId,
                1L,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                1500,
                "수정 비고입니다."
        );

        when(receiptService.updateReceiptItem(anyLong(), any(UpdateReceiptItemRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_수정시_입고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        UpdateReceiptItemRequest request = new UpdateReceiptItemRequest(
                receiptItemId,
                9999L,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                1500,
                "수정 비고입니다."
        );

        when(receiptService.updateReceiptItem(anyLong(), any(UpdateReceiptItemRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_NOT_FOUND));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_수정시_입고_접근_권한이_없으면_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        UpdateReceiptItemRequest request = new UpdateReceiptItemRequest(
                receiptItemId,
                1L,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                1500,
                "수정 비고입니다."
        );

        when(receiptService.updateReceiptItem(anyLong(), any(UpdateReceiptItemRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ACCESS_DENIED));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_수정시_아이템이_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 9999L;

        UpdateReceiptItemRequest request = new UpdateReceiptItemRequest(
                receiptItemId,
                1L,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                1500,
                "수정 비고입니다."
        );

        when(receiptService.updateReceiptItem(anyLong(), any(UpdateReceiptItemRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ITEM_NOT_FOUND));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ITEM_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ITEM_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_수정시_다른_상점의_아이템이면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        UpdateReceiptItemRequest request = new UpdateReceiptItemRequest(
                receiptItemId,
                1L,
                2,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(5),
                10,
                1500,
                "수정 비고입니다."
        );

        when(receiptService.updateReceiptItem(anyLong(), any(UpdateReceiptItemRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED));

        String body = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_삭제에_성공한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        when(receiptService.deleteReceiptItem(anyLong(), anyLong()))
                .thenReturn(new DeleteReceiptItemResponse(true));

        // when & then
        mockMvc.perform(delete("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.DELETE_RECEIPT_ITEM_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void 입고_아이템_삭제시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        when(receiptService.deleteReceiptItem(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(delete("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_아이템_삭제시_다른_상점의_아이템이면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long receiptItemId = 1000L;

        when(receiptService.deleteReceiptItem(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED));

        // when & then
        mockMvc.perform(delete("/api/v1/receipt/receipt/{receiptItemId}", receiptItemId)
                        .with(authentication(auth())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ITEM_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_확정에_성공한다() throws Exception {
        // given
        Long receiptId = 1L;

        ConfirmReceiptResponse response = new ConfirmReceiptResponse(true);

        when(receiptService.confirmReceipt(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}/confirm", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.CONFIRM_RECEIPT_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void 입고_확정시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 1L;

        when(receiptService.confirmReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}/confirm", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_확정시_입고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 9999L;

        when(receiptService.confirmReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}/confirm", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 입고_확정시_다른_상점의_입고면_접근_거부_예외가_발생한다() throws Exception {
        // given
        Long receiptId = 1L;

        when(receiptService.confirmReceipt(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.RECEIPT_ACCESS_DENIED));

        // when & then
        mockMvc.perform(patch("/api/v1/receipt/{receiptId}/confirm", receiptId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(ErrorCode.RECEIPT_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RECEIPT_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
