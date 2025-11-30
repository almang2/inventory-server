package com.almang.inventory.product.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.monitoring.DiscordErrorNotifier;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.product.domain.ProductUnit;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.request.UpdateProductRequest;
import com.almang.inventory.product.dto.response.DeleteProductResponse;
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

        @Autowired
        private MockMvc mockMvc;
        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private ProductService productService;
        @MockitoBean
        private JpaMetamodelMappingContext jpaMetamodelMappingContext;
        @MockitoBean
        private DiscordErrorNotifier discordErrorNotifier;

        private UsernamePasswordAuthenticationToken auth() {
                CustomUserPrincipal principal = new CustomUserPrincipal(1L, "store_admin", List.of());
                return new UsernamePasswordAuthenticationToken(
                                principal, null, principal.getAuthorities());
        }

        @Test
        void 품목_등록에_성공한다() throws Exception {
                // given
                CreateProductRequest request = new CreateProductRequest(
                                1L,
                                "고체치약",
                                "C-001",
                                "P-001",
                                ProductUnit.G,
                                BigDecimal.valueOf(1000.0),
                                10,
                                BigDecimal.valueOf(100.0),
                                1000,
                                1500,
                                1200);

                ProductResponse response = new ProductResponse(
                                1L,
                                "고체치약",
                                "C-001",
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
                                1L);

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
                                .andExpect(jsonPath("$.data.cafe24Code").value("C-001"))
                                .andExpect(jsonPath("$.data.posCode").value("P-001"))
                                .andExpect(jsonPath("$.data.unit").value(ProductUnit.G.name()))
                                .andExpect(jsonPath("$.data.isActivated").value(true))
                                .andExpect(jsonPath("$.data.storeId").value(1L))
                                .andExpect(jsonPath("$.data.vendorId").value(1L));
        }

        @Test
        void 품목_등록_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
                // given
                CreateProductRequest request = new CreateProductRequest(
                                1L,
                                "고체치약",
                                "C-001",
                                "P-001",
                                ProductUnit.G,
                                BigDecimal.valueOf(1000.0),
                                10,
                                BigDecimal.valueOf(100.0),
                                1000,
                                1500,
                                1200);

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
                                "C-001",
                                "P-001",
                                ProductUnit.G,
                                BigDecimal.valueOf(1000.0),
                                10,
                                BigDecimal.valueOf(100.0),
                                1000,
                                1500,
                                1200);

                when(productService.createProduct(any(CreateProductRequest.class), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.VENDOR_NOT_FOUND));

                // when & then
                mockMvc.perform(post("/api/v1/product")
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.VENDOR_NOT_FOUND.getHttpStatus().value()))
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
                                "",
                                null,
                                null,
                                0,
                                null,
                                0,
                                0,
                                0);

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

        @Test
        void 품목_수정에_성공한다() throws Exception {
                // given
                Long productId = 1L;

                UpdateProductRequest request = new UpdateProductRequest(
                                1L,
                                "수정된 고체치약",
                                "C-999",
                                "P-999",
                                ProductUnit.ML,
                                BigDecimal.valueOf(1200.0),
                                20,
                                BigDecimal.valueOf(110.0),
                                false,
                                2000,
                                2500,
                                2200);

                ProductResponse response = new ProductResponse(
                                productId,
                                "수정된 고체치약",
                                "C-999",
                                "P-999",
                                ProductUnit.ML,
                                BigDecimal.valueOf(1200.0),
                                false,
                                20,
                                BigDecimal.valueOf(110.0),
                                2000,
                                2500,
                                2200,
                                1L,
                                1L);

                when(productService.updateProduct(anyLong(), any(UpdateProductRequest.class), anyLong()))
                                .thenReturn(response);

                // when & then
                mockMvc.perform(patch("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message")
                                                .value(SuccessMessage.UPDATE_PRODUCT_SUCCESS.getMessage()))
                                .andExpect(jsonPath("$.data.name").value("수정된 고체치약"))
                                .andExpect(jsonPath("$.data.cafe24Code").value("C-999"))
                                .andExpect(jsonPath("$.data.posCode").value("P-999"))
                                .andExpect(jsonPath("$.data.unit").value(ProductUnit.ML.name()))
                                .andExpect(jsonPath("$.data.isActivated").value(false))
                                .andExpect(jsonPath("$.data.storeId").value(1L))
                                .andExpect(jsonPath("$.data.vendorId").value(1L));
        }

        @Test
        void 품목_수정_시_품목이_존재하지_않으면_예외가_발생한다() throws Exception {
                // given
                Long productId = 9999L;

                UpdateProductRequest request = new UpdateProductRequest(
                                1L,
                                "수정된 고체치약",
                                "C-999",
                                "P-999",
                                ProductUnit.ML,
                                BigDecimal.valueOf(1200.0),
                                20,
                                BigDecimal.valueOf(110.0),
                                false,
                                2000,
                                2500,
                                2200);

                when(productService.updateProduct(anyLong(), any(UpdateProductRequest.class), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

                // when & then
                mockMvc.perform(patch("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.PRODUCT_NOT_FOUND.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message")
                                                .value(ErrorCode.PRODUCT_NOT_FOUND.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        void 품목_수정_시_발주처가_존재하지_않으면_예외가_발생한다() throws Exception {
                // given
                Long productId = 1L;

                UpdateProductRequest request = new UpdateProductRequest(
                                9999L,
                                "수정된 고체치약",
                                "P-999",
                                ProductUnit.ML,
                                BigDecimal.valueOf(1200.0),
                                20,
                                BigDecimal.valueOf(110.0),
                                false,
                                2000,
                                2500,
                                2200);

                when(productService.updateProduct(anyLong(), any(UpdateProductRequest.class), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.VENDOR_NOT_FOUND));

                // when & then
                mockMvc.perform(patch("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.VENDOR_NOT_FOUND.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message")
                                                .value(ErrorCode.VENDOR_NOT_FOUND.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        void 품목_수정_시_다른_상점_발주처이면_예외가_발생한다() throws Exception {
                // given
                Long productId = 1L;

                UpdateProductRequest request = new UpdateProductRequest(
                                2L,
                                "수정된 고체치약",
                                "C-999",
                                "P-999",
                                ProductUnit.ML,
                                BigDecimal.valueOf(1200.0),
                                20,
                                BigDecimal.valueOf(110.0),
                                false,
                                2000,
                                2500,
                                2200);

                when(productService.updateProduct(anyLong(), any(UpdateProductRequest.class), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.VENDOR_ACCESS_DENIED));

                // when & then
                mockMvc.perform(patch("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.VENDOR_ACCESS_DENIED.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message")
                                                .value(ErrorCode.VENDOR_ACCESS_DENIED.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        void 품목_수정_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
                // given
                UpdateProductRequest invalidRequest = new UpdateProductRequest(
                                null,
                                "",
                                "",
                                "",
                                null,
                                null,
                                0,
                                null,
                                true,
                                -1,
                                -1,
                                -1);

                // when & then
                mockMvc.perform(patch("/api/v1/product/{productId}", 1L)
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
        void 품목_상세_조회에_성공한다() throws Exception {
                // given
                Long productId = 1L;

                ProductResponse response = new ProductResponse(
                                productId,
                                "고체치약",
                                "C-001",
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
                                1L);

                when(productService.getProductDetail(anyLong(), anyLong()))
                                .thenReturn(response);

                // when & then
                mockMvc.perform(get("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message")
                                                .value(SuccessMessage.GET_PRODUCT_DETAIL_SUCCESS.getMessage()))
                                .andExpect(jsonPath("$.data.productId").value(1L))
                                .andExpect(jsonPath("$.data.name").value("고체치약"))
                                .andExpect(jsonPath("$.data.cafe24Code").value("C-001"))
                                .andExpect(jsonPath("$.data.posCode").value("P-001"))
                                .andExpect(jsonPath("$.data.unit").value(ProductUnit.G.name()))
                                .andExpect(jsonPath("$.data.isActivated").value(true))
                                .andExpect(jsonPath("$.data.storeId").value(1L))
                                .andExpect(jsonPath("$.data.vendorId").value(1L));
        }

        @Test
        void 품목_상세_조회_시_품목이_존재하지_않으면_예외가_발생한다() throws Exception {
                // given
                Long notExistProductId = 9999L;

                when(productService.getProductDetail(anyLong(), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

                // when & then
                mockMvc.perform(get("/api/v1/product/{productId}", notExistProductId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.PRODUCT_NOT_FOUND.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message")
                                                .value(ErrorCode.PRODUCT_NOT_FOUND.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        void 품목_상세_조회_시_다른_상점_품목이면_예외가_발생한다() throws Exception {
                // given
                Long productId = 1L;

                when(productService.getProductDetail(anyLong(), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.STORE_ACCESS_DENIED));

                // when & then
                mockMvc.perform(get("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.STORE_ACCESS_DENIED.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message")
                                                .value(ErrorCode.STORE_ACCESS_DENIED.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        void 품목_목록_조회에_성공한다() throws Exception {
                // given
                ProductResponse product1 = new ProductResponse(
                                1L,
                                "고체치약",
                                "C-001",
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
                                1L);

                ProductResponse product2 = new ProductResponse(
                                2L,
                                "고무장갑",
                                "C-002",
                                "P-002",
                                ProductUnit.G,
                                BigDecimal.valueOf(800.0),
                                true,
                                20,
                                BigDecimal.valueOf(40.0),
                                500,
                                1000,
                                800,
                                1L,
                                2L);

                PageResponse<ProductResponse> pageResponse = new PageResponse<>(
                                List.of(product1, product2),
                                1,
                                10,
                                2L,
                                1,
                                true);

                when(productService.getProductList(anyLong(), any(), any(), any(), any()))
                                .thenReturn(pageResponse);

                // when & then
                mockMvc.perform(get("/api/v1/product")
                                .with(authentication(auth()))
                                .param("page", "1")
                                .param("size", "10")
                                .param("isActivate", "true")
                                .param("name", "고"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message")
                                                .value(SuccessMessage.GET_PRODUCT_LIST_SUCCESS.getMessage()))
                                .andExpect(jsonPath("$.data.page").value(1))
                                .andExpect(jsonPath("$.data.size").value(10))
                                .andExpect(jsonPath("$.data.totalElements").value(2))
                                .andExpect(jsonPath("$.data.totalPages").value(1))
                                .andExpect(jsonPath("$.data.last").value(true))
                                .andExpect(jsonPath("$.data.content[0].productId").value(1L))
                                .andExpect(jsonPath("$.data.content[0].name").value("고체치약"))
                                .andExpect(jsonPath("$.data.content[0].cafe24Code").value("C-001"))
                                .andExpect(jsonPath("$.data.content[0].posCode").value("P-001"))
                                .andExpect(jsonPath("$.data.content[0].unit").value(ProductUnit.G.name()))
                                .andExpect(jsonPath("$.data.content[0].isActivated").value(true))
                                .andExpect(jsonPath("$.data.content[0].storeId").value(1L))
                                .andExpect(jsonPath("$.data.content[0].vendorId").value(1L));
        }

        @Test
        void 품목_목록_조회_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
                // given
                when(productService.getProductList(anyLong(), any(), any(), any(), any()))
                                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

                // when & then
                mockMvc.perform(get("/api/v1/product")
                                .with(authentication(auth()))
                                .param("page", "1")
                                .param("size", "10"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message")
                                                .value(ErrorCode.USER_NOT_FOUND.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        void 품목_삭제에_성공한다() throws Exception {
                // given
                Long productId = 1L;
                DeleteProductResponse response = new DeleteProductResponse(true);

                when(productService.deleteProduct(anyLong(), anyLong()))
                                .thenReturn(response);

                // when & then
                mockMvc.perform(delete("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value(200))
                                .andExpect(jsonPath("$.message")
                                                .value(SuccessMessage.DELETE_PRODUCT_SUCCESS.getMessage()))
                                .andExpect(jsonPath("$.data.success").value(true));
        }

        @Test
        void 품목_삭제_시_품목이_존재하지_않으면_예외가_발생한다() throws Exception {
                // given
                Long notExistProductId = 9999L;

                when(productService.deleteProduct(anyLong(), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

                // when & then
                mockMvc.perform(delete("/api/v1/product/{productId}", notExistProductId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.PRODUCT_NOT_FOUND.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message").value(ErrorCode.PRODUCT_NOT_FOUND.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        void 품목_삭제_시_다른_상점_품목이면_예외가_발생한다() throws Exception {
                // given
                Long productId = 1L;

                when(productService.deleteProduct(anyLong(), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.STORE_ACCESS_DENIED));

                // when & then
                mockMvc.perform(delete("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status")
                                                .value(ErrorCode.STORE_ACCESS_DENIED.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message").value(ErrorCode.STORE_ACCESS_DENIED.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        void 품목_삭제_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
                // given
                Long productId = 1L;

                when(productService.deleteProduct(anyLong(), anyLong()))
                                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

                // when & then
                mockMvc.perform(delete("/api/v1/product/{productId}", productId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                                .andExpect(jsonPath("$.data").doesNotExist());
        }
}
