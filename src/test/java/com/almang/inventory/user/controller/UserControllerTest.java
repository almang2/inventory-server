package com.almang.inventory.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.store.global.config.TestSecurityConfig;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.dto.request.UpdateUserProfileRequest;
import com.almang.inventory.user.dto.response.UpdateUserProfileResponse;
import com.almang.inventory.user.dto.response.UserProfileResponse;
import com.almang.inventory.user.service.UserService;
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

@WebMvcTest(UserController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    private UsernamePasswordAuthenticationToken auth() {
        CustomUserPrincipal principal =
                new CustomUserPrincipal(1L, "store_admin", List.of());
        return new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
    }

    @Test
    void 사용자_정보_조회에_성공한다() throws Exception {
        // given
        Long userId = 1L;

        UserProfileResponse response = new UserProfileResponse(
                "store_admin",
                "상점 관리자",
                UserRole.ADMIN,
                "테스트 상점"
        );

        when(userService.getUserProfile(anyLong()))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/me")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.GET_USER_PROFILE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.username").value("store_admin"))
                .andExpect(jsonPath("$.data.name").value("상점 관리자"))
                .andExpect(jsonPath("$.data.role").value(UserRole.ADMIN.name()))
                .andExpect(jsonPath("$.data.storeName").value("테스트 상점"));
    }

    @Test
    void 사용자_정보_조회_시_사용자를_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        Long userId = 1L;


        when(userService.getUserProfile(anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/users/me")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 사용자_프로필_수정에_성공한다() throws Exception {
        // given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새로운 이름");
        UpdateUserProfileResponse response = new UpdateUserProfileResponse(true);

        when(userService.updateUserProfile(anyLong(), any(UpdateUserProfileRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value(SuccessMessage.UPDATE_USER_PROFILE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void 사용자_프로필_수정_시_사용자가_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("새로운 이름");

        when(userService.updateUserProfile(anyLong(), any(UpdateUserProfileRequest.class)))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 사용자_프로필_수정_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest("");

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 사용자_프로필_수정_요청_이름이_20자를_초과하면_예외가_발생한다() throws Exception {
        // given
        UpdateUserProfileRequest invalidRequest = new UpdateUserProfileRequest("123456789012345678901");

        when(userService.updateUserProfile(anyLong(), any(UpdateUserProfileRequest.class)))
                .thenThrow(new BaseException(ErrorCode.NAME_IS_LONG));

        // when & then
        mockMvc.perform(patch("/api/v1/users/me")
                        .with(authentication(auth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.NAME_IS_LONG.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.NAME_IS_LONG.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
