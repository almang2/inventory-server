package com.almang.inventory.order.template.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.order.template.dto.request.UpdateOrderTemplateRequest;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.order.template.service.OrderTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@WebMvcTest(OrderTemplateController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class OrderTemplateControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrderTemplateService orderTemplateService;
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
    void 발주_템플릿_수정에_성공한다() throws Exception {
        // given
        Long orderTemplateId = 1L;

        UpdateOrderTemplateRequest request = new UpdateOrderTemplateRequest(
                10L,
                "수정된 제목",
                "수정된 본문 내용입니다.",
                false
        );

        OrderTemplateResponse response = new OrderTemplateResponse(
                orderTemplateId,
                10L,
                "수정된 제목",
                "수정된 본문 내용입니다.",
                false
        );

        when(orderTemplateService.updateOrderTemplate(anyLong(), any(UpdateOrderTemplateRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/order-template/{orderTemplateId}", orderTemplateId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.UPDATE_ORDER_TEMPLATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.orderTemplateId").value(orderTemplateId))
                .andExpect(jsonPath("$.data.vendorId").value(10L))
                .andExpect(jsonPath("$.data.title").value("수정된 제목"))
                .andExpect(jsonPath("$.data.body").value("수정된 본문 내용입니다."))
                .andExpect(jsonPath("$.data.activated").value(false));
    }

    @Test
    void 발주_템플릿_수정_시_존재하지_않는_템플릿이면_예외가_발생한다() throws Exception {
        // given
        Long notExistOrderTemplateId = 9999L;

        UpdateOrderTemplateRequest request = new UpdateOrderTemplateRequest(
                10L,
                "수정 제목",
                "수정 본문",
                true
        );

        when(orderTemplateService.updateOrderTemplate(anyLong(), any(UpdateOrderTemplateRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.ORDER_TEMPLATE_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/order-template/{orderTemplateId}", notExistOrderTemplateId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.ORDER_TEMPLATE_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorCode.ORDER_TEMPLATE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_템플릿_수정_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        UpdateOrderTemplateRequest invalidRequest = new UpdateOrderTemplateRequest(
                null,
                "",
                "",
                true
        );

        // when & then
        mockMvc.perform(patch("/api/v1/order-template/{orderTemplateId}", 1L)
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
    void 발주_템플릿_수정_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long orderTemplateId = 1L;

        UpdateOrderTemplateRequest request = new UpdateOrderTemplateRequest(
                10L,
                "제목",
                "본문",
                true
        );

        when(orderTemplateService.updateOrderTemplate(anyLong(), any(UpdateOrderTemplateRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/order-template/{orderTemplateId}", orderTemplateId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
