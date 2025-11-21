package com.almang.inventory.vendor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.vendor.domain.VendorChannel;
import com.almang.inventory.vendor.dto.request.CreateVendorRequest;
import com.almang.inventory.vendor.dto.request.UpdateVendorRequest;
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

    @Test
    void 발주처_수정에_성공한다() throws Exception {
        // given
        Long vendorId = 1L;

        UpdateVendorRequest request = new UpdateVendorRequest(
                "수정된 발주처",
                VendorChannel.EMAIL,
                "vendor-updated@test.com",
                "수정 메모",
                false
        );

        VendorResponse response = new VendorResponse(
                vendorId,
                "수정된 발주처",
                VendorChannel.EMAIL,
                "vendor-updated@test.com",
                "수정 메모",
                false,
                1L
        );

        when(vendorService.updateVendor(anyLong(), any(UpdateVendorRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/vendor/{vendorId}", vendorId)
                                .with(authentication(auth()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.UPDATE_VENDOR_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.vendorId").value(vendorId))
                .andExpect(jsonPath("$.data.name").value("수정된 발주처"))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.contactPoint").value("vendor-updated@test.com"))
                .andExpect(jsonPath("$.data.note").value("수정 메모"))
                .andExpect(jsonPath("$.data.activated").value(false));
    }

    @Test
    void 발주처_수정_시_존재하지_않는_발주처면_예외를_던진다() throws Exception {
        // given
        Long vendorId = 9999L;

        UpdateVendorRequest request = new UpdateVendorRequest(
                "수정 요청",
                VendorChannel.KAKAO,
                "010-9999-9999",
                "존재하지 않는 발주처",
                true
        );

        when(vendorService.updateVendor(anyLong(), any(UpdateVendorRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/vendor/{vendorId}", vendorId)
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
    void 발주처_수정_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        UpdateVendorRequest invalidRequest = new UpdateVendorRequest(
                "1234567890123456789012345678901",
                VendorChannel.EMAIL,
                "",
                "메모",
                true
        );

        // when & then
        mockMvc.perform(patch("/api/v1/vendor/{vendorId}", 1L)
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
    void 발주처_상세_조회에_성공한다() throws Exception {
        // given
        Long vendorId = 1L;

        VendorResponse response = new VendorResponse(
                vendorId,
                "테스트 발주처",
                VendorChannel.KAKAO,
                "010-1111-2222",
                "메모",
                true,
                1L
        );

        when(vendorService.getVendorDetail(anyLong(), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vendor/{vendorId}", vendorId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_VENDOR_DETAIL_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.vendorId").value(vendorId))
                .andExpect(jsonPath("$.data.name").value("테스트 발주처"))
                .andExpect(jsonPath("$.data.channel").value("KAKAO"))
                .andExpect(jsonPath("$.data.contactPoint").value("010-1111-2222"))
                .andExpect(jsonPath("$.data.note").value("메모"))
                .andExpect(jsonPath("$.data.storeId").value(1L))
                .andExpect(jsonPath("$.data.activated").value(true));
    }

    @Test
    void 발주처_상세_조회_시_존재하지_않는_발주처면_예외가_발생한다() throws Exception {
        // given
        Long vendorId = 9999L;

        when(vendorService.getVendorDetail(anyLong(), anyLong()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/vendor/{vendorId}", vendorId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.VENDOR_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.VENDOR_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}