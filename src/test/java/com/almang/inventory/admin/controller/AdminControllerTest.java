package com.almang.inventory.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.admin.dto.request.CreateStoreRequest;
import com.almang.inventory.admin.dto.response.CreateStoreResponse;
import com.almang.inventory.admin.service.AdminService;
import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.config.TestSecurityConfig;
import com.almang.inventory.global.monitoring.DiscordErrorNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AdminService adminService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;
    @MockitoBean private DiscordErrorNotifier discordErrorNotifier;

    @Test
    void 상점_생성에_성공한다() throws Exception {
        // given
        CreateStoreRequest request = new CreateStoreRequest(
                "테스트 상점"
        );

        CreateStoreResponse response = new CreateStoreResponse(
                1L,
                "테스트 상점",
                true
        );

        when(adminService.createStore(any(CreateStoreRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/admin/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.CREATE_STORE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.storeId").value(1L))
                .andExpect(jsonPath("$.data.name").value("테스트 상점"))
                .andExpect(jsonPath("$.data.isActivate").value(true));
    }

    @Test
    void 상점_생성_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        CreateStoreRequest invalidRequest = new CreateStoreRequest(
                ""
        );

        // when & then
        mockMvc.perform(post("/api/v1/admin/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}