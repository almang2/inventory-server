package com.almang.inventory.store.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.store.dto.request.UpdateStoreRequest;
import com.almang.inventory.store.dto.response.UpdateStoreResponse;
import com.almang.inventory.store.service.StoreService;
import com.almang.inventory.store.global.config.TestSecurityConfig;
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

@WebMvcTest(StoreController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class StoreControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private StoreService storeService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UsernamePasswordAuthenticationToken auth() {
        CustomUserPrincipal principal =
                new CustomUserPrincipal(1L, "store_admin", List.of());
        return new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
    }

    @Test
    void 상점_정보_업데이트에_성공한다() throws Exception {
        // given
        UpdateStoreRequest request = new UpdateStoreRequest(
                "수정된 상점",
                BigDecimal.valueOf(0.5),
                false
        );

        UpdateStoreResponse response = new UpdateStoreResponse(
                1L,
                "수정된 상점",
                false,
                BigDecimal.valueOf(0.5)
        );

        when(storeService.updateStore(any(UpdateStoreRequest.class), anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/store")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.UPDATE_STORE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.name").value("수정된 상점"))
                .andExpect(jsonPath("$.data.defaultCountCheckThreshold").value(0.5))
                .andExpect(jsonPath("$.data.isActivate").value(false));
    }

    @Test
    void 상점_정보_업데이트_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        UpdateStoreRequest request = new UpdateStoreRequest(
                "아무이름",
                BigDecimal.valueOf(0.3),
                true
        );

        when(storeService.updateStore(any(UpdateStoreRequest.class), anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/store")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 상점_정보_업데이트_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        UpdateStoreRequest invalidRequest = new UpdateStoreRequest(
                "123456789012345678901",  // 21자
                BigDecimal.valueOf(0.3),
                true
        );

        // when & then
        mockMvc.perform(patch("/api/v1/store")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
