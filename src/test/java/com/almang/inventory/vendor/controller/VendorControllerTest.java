package com.almang.inventory.vendor.controller;

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
import com.almang.inventory.vendor.domain.VendorChannel;
import com.almang.inventory.vendor.dto.request.CreateVendorRequest;
import com.almang.inventory.vendor.dto.response.VendorResponse;
import com.almang.inventory.vendor.service.VendorService;
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

@WebMvcTest(VendorController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class VendorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private VendorService vendorService;
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
    void 발주처_등록에_성공한다() throws Exception {
        // given
        CreateVendorRequest request = new CreateVendorRequest(
                "테스트 발주처",
                VendorChannel.KAKAO,
                "010-1111-2222",
                "비고"
        );

        VendorResponse response = new VendorResponse(
                1L,
                "테스트 발주처",
                VendorChannel.KAKAO,
                "010-1111-2222",
                "비고",
                true,
                1L
        );

        when(vendorService.createVendor(any(CreateVendorRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/vendor")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.CREATE_VENDOR_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.vendorId").value(1L))
                .andExpect(jsonPath("$.data.name").value("테스트 발주처"))
                .andExpect(jsonPath("$.data.channel").value(VendorChannel.KAKAO.name()))
                .andExpect(jsonPath("$.data.contactPoint").value("010-1111-2222"))
                .andExpect(jsonPath("$.data.note").value("비고"))
                .andExpect(jsonPath("$.data.storeId").value(1L))
                .andExpect(jsonPath("$.data.activated").value(true));
    }

    @Test
    void 발주처_등록_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        CreateVendorRequest request = new CreateVendorRequest(
                "테스트 발주처",
                VendorChannel.KAKAO,
                "010-1111-2222",
                "비고"
        );

        when(vendorService.createVendor(any(CreateVendorRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/vendor")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주처_등록_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        CreateVendorRequest invalidRequest = new CreateVendorRequest(
                "",
                null,
                "",
                "비고"
        );

        // when & then
        mockMvc.perform(post("/api/v1/vendor")
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