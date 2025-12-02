package com.almang.inventory.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.monitoring.DiscordErrorNotifier;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.inventory.domain.InventoryMoveDirection;
import com.almang.inventory.inventory.domain.InventoryStatus;
import com.almang.inventory.inventory.dto.request.MoveInventoryRequest;
import com.almang.inventory.inventory.dto.request.UpdateInventoryRequest;
import com.almang.inventory.inventory.dto.response.InventoryResponse;
import com.almang.inventory.inventory.service.InventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class InventoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private InventoryService inventoryService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean private DiscordErrorNotifier discordErrorNotifier;

    private UsernamePasswordAuthenticationToken auth() {
        CustomUserPrincipal principal =
                new CustomUserPrincipal(1L, "inventory_admin", List.of());
        return new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
    }

    @Test
    void 재고_수동_수정에_성공한다() throws Exception {
        // given
        Long inventoryId = 1L;
        Long productId = 10L;

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                productId,
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(10.000),
                BigDecimal.valueOf(0.500),
                BigDecimal.valueOf(3.000),
                BigDecimal.valueOf(0.25)
        );

        InventoryResponse response = new InventoryResponse(
                inventoryId,
                productId,
                "상품1",
                "P001",
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(10.000),
                BigDecimal.valueOf(0.500),
                BigDecimal.valueOf(3.000),
                BigDecimal.valueOf(0.25),
                InventoryStatus.NORMAL
        );

        when(inventoryService.updateInventory(anyLong(), any(UpdateInventoryRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/inventory/{inventoryId}", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.UPDATE_INVENTORY_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.inventoryId").value(inventoryId))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.displayStock").value(1.234))
                .andExpect(jsonPath("$.data.warehouseStock").value(10.000))
                .andExpect(jsonPath("$.data.incomingReserved").value(3.000))
                .andExpect(jsonPath("$.data.reorderTriggerPoint").value(0.25))
                .andExpect(jsonPath("$.data.inventoryStatus").value(InventoryStatus.NORMAL.name()));
    }

    @Test
    void 재고_수동_수정시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long inventoryId = 1L;

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                10L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.2)
        );

        when(inventoryService.updateInventory(anyLong(), any(UpdateInventoryRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/inventory/{inventoryId}", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 재고_수동_수정시_재고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long inventoryId = 9999L;

        UpdateInventoryRequest request = new UpdateInventoryRequest(
                10L,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.2)
        );

        when(inventoryService.updateInventory(anyLong(), any(UpdateInventoryRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.INVENTORY_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/inventory/{inventoryId}", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVENTORY_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVENTORY_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 재고_수동_수정_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        Long inventoryId = 1L;

        UpdateInventoryRequest invalidRequest = new UpdateInventoryRequest(
                null,
                BigDecimal.valueOf(-1.0),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1.5)
        );

        // when & then
        mockMvc.perform(patch("/api/v1/inventory/{inventoryId}", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 재고_ID_기반_재고_조회에_성공한다() throws Exception {
        // given
        Long inventoryId = 1L;
        Long productId = 10L;

        InventoryResponse response = new InventoryResponse(
                inventoryId,
                productId,
                "상품1",
                "P001",
                BigDecimal.valueOf(1.234),
                BigDecimal.valueOf(10.000),
                BigDecimal.valueOf(0.500),
                BigDecimal.valueOf(3.000),
                BigDecimal.valueOf(0.25),
                InventoryStatus.NORMAL
        );

        when(inventoryService.getInventory(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/inventory/{inventoryId}", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_INVENTORY_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.inventoryId").value(inventoryId))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.displayStock").value(1.234))
                .andExpect(jsonPath("$.data.warehouseStock").value(10.000))
                .andExpect(jsonPath("$.data.incomingReserved").value(3.000))
                .andExpect(jsonPath("$.data.reorderTriggerPoint").value(0.25))
                .andExpect(jsonPath("$.data.inventoryStatus").value(InventoryStatus.NORMAL.name()));
    }

    @Test
    void 재고_ID_기반_재고_조회시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long inventoryId = 1L;

        when(inventoryService.getInventory(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/inventory/{inventoryId}", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 재고_ID_기반_재고_조회시_재고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long inventoryId = 9999L;

        when(inventoryService.getInventory(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.INVENTORY_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/inventory/{inventoryId}", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVENTORY_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVENTORY_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 품목_ID_기반_재고_조회에_성공한다() throws Exception {
        // given
        Long inventoryId = 1L;
        Long productId = 10L;

        InventoryResponse response = new InventoryResponse(
                inventoryId,
                productId,
                "상품1",
                "P001",
                BigDecimal.valueOf(1.000),
                BigDecimal.valueOf(5.000),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.3),
                InventoryStatus.NORMAL
        );

        when(inventoryService.getInventoryByProduct(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/inventory/product/{productId}", productId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_INVENTORY_BY_PRODUCT_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.inventoryId").value(inventoryId))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.displayStock").value(1.000))
                .andExpect(jsonPath("$.data.warehouseStock").value(5.000))
                .andExpect(jsonPath("$.data.reorderTriggerPoint").value(0.3))
                .andExpect(jsonPath("$.data.inventoryStatus").value(InventoryStatus.NORMAL.name()));
    }

    @Test
    void 품목_ID_기반_재고_조회시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long productId = 10L;

        when(inventoryService.getInventoryByProduct(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/inventory/product/{productId}", productId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 품목_ID_기반_재고_조회시_재고가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long productId = 9999L;

        when(inventoryService.getInventoryByProduct(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.INVENTORY_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/inventory/product/{productId}", productId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVENTORY_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVENTORY_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 상점_재고_목록_조회에_성공한다() throws Exception {
        // given
        Long inventoryId = 1L;
        Long productId = 10L;

        InventoryResponse inventoryResponse = new InventoryResponse(
                inventoryId,
                productId,
                "상품1",
                "P001",
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.2),
                InventoryStatus.NORMAL
        );

        Page<InventoryResponse> page = new PageImpl<>(
                List.of(inventoryResponse),
                PageRequest.of(0, 20),
                1
        );

        PageResponse<InventoryResponse> pageResponse = PageResponse.from(page);

        when(inventoryService.getStoreInventoryList(anyLong(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/v1/inventory")
                        .param("page", "0")
                        .param("size", "20")
                        .param("scope", "all")
                        .param("q", "")
                        .param("sort", "")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.GET_STORE_INVENTORY_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content[0].inventoryId").value(inventoryId))
                .andExpect(jsonPath("$.data.content[0].productId").value(productId))
                .andExpect(jsonPath("$.data.content[0].productName").value("상품1"))
                .andExpect(jsonPath("$.data.content[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data.content[0].inventoryStatus").value(InventoryStatus.NORMAL.name()));
    }

    @Test
    void 재고_이동에_성공한다() throws Exception {
        // given
        Long inventoryId = 1L;
        Long productId = 10L;

        MoveInventoryRequest request = new MoveInventoryRequest(
                BigDecimal.valueOf(3), InventoryMoveDirection.WAREHOUSE_TO_DISPLAY
        );

        InventoryResponse response = new InventoryResponse(
                inventoryId,
                productId,
                "상품1",
                "P001",
                BigDecimal.valueOf(3),
                BigDecimal.valueOf(7),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.valueOf(0.2),
                InventoryStatus.NORMAL
        );

        when(inventoryService.moveInventory(anyLong(), any(MoveInventoryRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/inventory/{inventoryId}/move", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.MOVE_INVENTORY_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.inventoryId").value(inventoryId))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.productName").value("상품1"))
                .andExpect(jsonPath("$.data.productCode").value("P001"))
                .andExpect(jsonPath("$.data.displayStock").value(3))
                .andExpect(jsonPath("$.data.warehouseStock").value(7))
                .andExpect(jsonPath("$.data.inventoryStatus").value(InventoryStatus.NORMAL.name()));
    }

    @Test
    void 재고_이동시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long inventoryId = 1L;

        MoveInventoryRequest request = new MoveInventoryRequest(
                BigDecimal.ONE, InventoryMoveDirection.WAREHOUSE_TO_DISPLAY
        );

        when(inventoryService.moveInventory(anyLong(), any(MoveInventoryRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/inventory/{inventoryId}/move", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 재고_이동시_재고_도메인_예외가_발생하면_에러_응답을_반환한다() throws Exception {
        // given
        Long inventoryId = 1L;

        MoveInventoryRequest request = new MoveInventoryRequest(
                BigDecimal.TEN, InventoryMoveDirection.WAREHOUSE_TO_DISPLAY
        );

        when(inventoryService.moveInventory(anyLong(), any(MoveInventoryRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.DISPLAY_STOCK_NOT_ENOUGH));

        // when & then
        mockMvc.perform(post("/api/v1/inventory/{inventoryId}/move", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.DISPLAY_STOCK_NOT_ENOUGH.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.DISPLAY_STOCK_NOT_ENOUGH.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 재고_이동_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        Long inventoryId = 1L;

        MoveInventoryRequest invalidRequest = new MoveInventoryRequest(
                BigDecimal.valueOf(-1), null
        );

        // when & then
        mockMvc.perform(post("/api/v1/inventory/{inventoryId}/move", inventoryId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
