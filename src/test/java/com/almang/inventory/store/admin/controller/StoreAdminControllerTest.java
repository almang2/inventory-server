package com.almang.inventory.store.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.admin.dto.request.StoreAdminCreateRequest;
import com.almang.inventory.store.admin.dto.response.StoreAdminCreateResponse;
import com.almang.inventory.store.admin.service.StoreAdminService;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreAdminController.class)
@Import(TestSecurityConfig.class)
public class StoreAdminControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean StoreAdminService storeAdminService;
    @MockitoBean JpaMetamodelMappingContext jpaMetamodelMappingContext;


    @Test
    void 상점_관리자_계정_생성에_성공한다() throws Exception {
        // given
        StoreAdminCreateResponse response = new StoreAdminCreateResponse(
                1L, "store_admin", "password123!", "관리자", 10L
        );

        when(storeAdminService.createStoreAdmin(any()))
                .thenReturn(response);

        StoreAdminCreateRequest request = new StoreAdminCreateRequest(
                "store_admin", "관리자", 100L
        );

        // when & then
        mockMvc.perform(post("/api/v1/store/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.SIGNUP_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.username").value("store_admin"))
                .andExpect(jsonPath("$.data.name").value("관리자"))
                .andExpect(jsonPath("$.data.storeId").value(10L));
    }

    @Test
    void 상점이_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        StoreAdminCreateRequest request = new StoreAdminCreateRequest(
                "store_admin", "관리자", 9999L
        );

        when(storeAdminService.createStoreAdmin(any()))
                .thenThrow(new BaseException(ErrorCode.STORE_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/store/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.STORE_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.STORE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void username이_중복되면_예외가_발생한다() throws Exception {
        // given
        StoreAdminCreateRequest request = new StoreAdminCreateRequest(
                "store_admin", "관리자", 100L
        );

        when(storeAdminService.createStoreAdmin(any()))
                .thenThrow(new BaseException(ErrorCode.DUPLICATE_USERNAME));

        // when & then
        mockMvc.perform(post("/api/v1/store/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(ErrorCode.DUPLICATE_USERNAME.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.DUPLICATE_USERNAME.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 상점_관리자_계정_생성_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        StoreAdminCreateRequest invalidRequest = new StoreAdminCreateRequest("", "", null);

        // when & then
        mockMvc.perform(post("/api/v1/store/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
