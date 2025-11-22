package com.almang.inventory.vendor.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
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
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.vendor.domain.VendorChannel;
import com.almang.inventory.vendor.dto.request.CreateOrderTemplateRequest;
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

    @Test
    void 발주처_목록_조회에_성공한다() throws Exception {
        // given
        PageResponse<VendorResponse> response = new PageResponse<>(
                List.of(
                        new VendorResponse(1L, "테스트1", VendorChannel.KAKAO, "010-1111-1111", "메모1", true, 1L),
                        new VendorResponse(2L, "테스트2", VendorChannel.EMAIL, "010-2222-2222", "메모2", true, 1L)
                ),
                1,
                20,
                2,
                1,
                true
        );

        when(vendorService.getVendorList(anyLong(), any(), any(), any(), any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vendor")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_VENDOR_LIST_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void 발주처_목록_조회_시_활성_필터가_적용된다() throws Exception {
        // given
        PageResponse<VendorResponse> response = new PageResponse<>(
                List.of(
                        new VendorResponse(1L, "활성 발주처", VendorChannel.KAKAO, "010-3333-3333", "메모", true, 1L)
                ),
                1,
                20,
                1,
                1,
                true
        );

        when(vendorService.getVendorList(anyLong(), any(), any(), any(), any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vendor")
                        .with(authentication(auth()))
                        .param("isActivate", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_VENDOR_LIST_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].activated").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("활성 발주처"));
    }

    @Test
    void 발주처_목록_조회_시_이름검색_필터가_적용된다() throws Exception {
        // given
        PageResponse<VendorResponse> response = new PageResponse<>(
                List.of(
                        new VendorResponse(1L, "사과 공장", VendorChannel.KAKAO, "010-4444-4444", "메모", true, 1L)
                ),
                1,
                20,
                1,
                1,
                true
        );

        when(vendorService.getVendorList(anyLong(), any(), any(), any(), any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vendor")
                        .with(authentication(auth()))
                        .param("name", "사과")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_VENDOR_LIST_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("사과 공장"));
    }

    @Test
    void 발주처_목록_조회_시_비활성_필터가_적용된다() throws Exception {
        // given
        PageResponse<VendorResponse> response = new PageResponse<>(
                List.of(
                        new VendorResponse(
                                1L,
                                "비활성 발주처",
                                VendorChannel.KAKAO,
                                "010-3333-3333",
                                "메모",
                                false,
                                1L
                        )
                ),
                1,
                20,
                1,
                1,
                true
        );

        when(vendorService.getVendorList(anyLong(), any(), any(), any(), any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vendor")
                        .with(authentication(auth()))
                        .param("isActivate", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_VENDOR_LIST_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].activated").value(false))
                .andExpect(jsonPath("$.data.content[0].name").value("비활성 발주처"));
    }

    @Test
    void 발주_템플릿_생성에_성공한다() throws Exception {
        // given
        Long vendorId = 1L;

        CreateOrderTemplateRequest request = new CreateOrderTemplateRequest(
                "발주 제목",
                "발주 본문 내용입니다."
        );

        OrderTemplateResponse response = new OrderTemplateResponse(
                1L,
                vendorId,
                "발주 제목",
                "발주 본문 내용입니다.",
                true
        );

        when(vendorService.createOrderTemplate(anyLong(), any(CreateOrderTemplateRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/vendor/{vendorId}/order-template", vendorId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.CREATE_ORDER_TEMPLATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.orderTemplateId").value(1L))
                .andExpect(jsonPath("$.data.title").value("발주 제목"))
                .andExpect(jsonPath("$.data.body").value("발주 본문 내용입니다."))
                .andExpect(jsonPath("$.data.activated").value(true))
                .andExpect(jsonPath("$.data.vendorId").value(vendorId));
    }

    @Test
    void 발주_템플릿_생성_시_존재하지_않는_발주처면_예외가_발생한다() throws Exception {
        // given
        Long vendorId = 9999L;

        CreateOrderTemplateRequest request = new CreateOrderTemplateRequest(
                "발주 제목",
                "발주 본문 내용입니다."
        );

        when(vendorService.createOrderTemplate(anyLong(), any(CreateOrderTemplateRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/vendor/{vendorId}/order-template", vendorId)
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
    void 발주_템플릿_생성_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        CreateOrderTemplateRequest invalidRequest = new CreateOrderTemplateRequest(
                "",
                ""
        );

        // when & then
        mockMvc.perform(post("/api/v1/vendor/{vendorId}/order-template", 1L)
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
    void 발주_템플릿_목록_조회에_성공한다() throws Exception {
        // given
        Long vendorId = 1L;

        List<OrderTemplateResponse> response = List.of(
                new OrderTemplateResponse(1L, vendorId, "템플릿1", "본문1", true),
                new OrderTemplateResponse(2L, vendorId, "템플릿2", "본문2", false)
        );

        when(vendorService.getOrderTemplates(anyLong(), anyLong(), any()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vendor/{vendorId}/order-templates", vendorId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_VENDOR_ORDER_TEMPLATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].orderTemplateId").value(1L))
                .andExpect(jsonPath("$.data[0].vendorId").value(vendorId))
                .andExpect(jsonPath("$.data[0].title").value("템플릿1"))
                .andExpect(jsonPath("$.data[0].body").value("본문1"))
                .andExpect(jsonPath("$.data[0].activated").value(true))
                .andExpect(jsonPath("$.data[1].orderTemplateId").value(2L))
                .andExpect(jsonPath("$.data[1].activated").value(false));
    }

    @Test
    void 발주_템플릿_목록_조회_시_비활성_필터가_적용된다() throws Exception {
        // given
        Long vendorId = 1L;

        List<OrderTemplateResponse> response = List.of(
                new OrderTemplateResponse(1L, vendorId, "비활성 템플릿", "본문", false)
        );

        when(vendorService.getOrderTemplates(anyLong(), anyLong(), eq(false)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/vendor/{vendorId}/order-templates", vendorId)
                        .with(authentication(auth()))
                        .param("activated", "false")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.GET_VENDOR_ORDER_TEMPLATE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].activated").value(false))
                .andExpect(jsonPath("$.data[0].title").value("비활성 템플릿"));
    }

    @Test
    void 발주_템플릿_목록_조회_시_존재하지_않는_발주처면_예외가_발생한다() throws Exception {
        // given
        Long vendorId = 9999L;

        when(vendorService.getOrderTemplates(anyLong(), anyLong(), any()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/vendor/{vendorId}/order-templates", vendorId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.VENDOR_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorCode.VENDOR_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 발주_템플릿_목록_조회_시_다른_상점_발주처면_예외가_발생한다() throws Exception {
        // given
        Long vendorId = 2L;

        when(vendorService.getOrderTemplates(anyLong(), anyLong(), any()))
                .thenThrow(new BaseException(ErrorCode.VENDOR_ACCESS_DENIED));

        // when & then
        mockMvc.perform(get("/api/v1/vendor/{vendorId}/order-templates", vendorId)
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status")
                        .value(ErrorCode.VENDOR_ACCESS_DENIED.getHttpStatus().value()))
                .andExpect(jsonPath("$.message")
                        .value(ErrorCode.VENDOR_ACCESS_DENIED.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}