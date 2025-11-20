package com.almang.inventory.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.product.domain.ProductUnit;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.response.ProductResponse;
import com.almang.inventory.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

@WebMvcTest(ProductController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProductService productService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UsernamePasswordAuthenticationToken auth() {
        CustomUserPrincipal principal =
                new CustomUserPrincipal(1L, "store_admin", List.of());
        return new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
    }

    @Test
    void 품목_등록에_성공한다() throws Exception {
        // given
        CreateProductRequest request = new CreateProductRequest(
                1L,
                "고체치약",
                "P-001",
                ProductUnit.G,
                BigDecimal.valueOf(1000.0),
                10,
                BigDecimal.valueOf(100.0),
                1000,
                1500,
                1200
        );

        ProductResponse response = new ProductResponse(
                "고체치약",
                "P-001",
                ProductUnit.G,
                BigDecimal.valueOf(1000.0),
                true,
                10,
                BigDecimal.valueOf(100.0),
                1000,
                1500,
                1200,
                1L,
                1L
        );

        when(productService.createProduct(any(CreateProductRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/product")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.CREATE_PRODUCT_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.name").value("고체치약"))
                .andExpect(jsonPath("$.data.code").value("P-001"))
                .andExpect(jsonPath("$.data.unit").value(ProductUnit.G.name()))
                .andExpect(jsonPath("$.data.isActivate").value(true))
                .andExpect(jsonPath("$.data.storeId").value(1L))
                .andExpect(jsonPath("$.data.vendorId").value(1L));
    }

    @Test
    void 품목_등록_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        CreateProductRequest request = new CreateProductRequest(
                1L,
                "고체치약",
                "P-001",
                ProductUnit.G,
                BigDecimal.valueOf(1000.0),
                10,
                BigDecimal.valueOf(100.0),
                1000,
                1500,
                1200
        );

        when(productService.createProduct(any(CreateProductRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/product")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 품목_등록_시_발주처가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        CreateProductRequest request = new CreateProductRequest(
                9999L,
                "고체치약",
                "P-001",
                ProductUnit.G,
                BigDecimal.valueOf(1000.0),
                10,
                BigDecimal.valueOf(100.0),
                1000,
                1500,
                1200
        );

        when(productService.createProduct(any(CreateProductRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/product")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.VENDOR_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.VENDOR_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 품목_등록_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        CreateProductRequest invalidRequest = new CreateProductRequest(
                null,
                "",
                "",
                null,
                null,
                0,
                null,
                0,
                0,
                0
        );

        // when & then
        mockMvc.perform(post("/api/v1/product")
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
}
