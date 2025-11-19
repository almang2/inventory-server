package com.almang.inventory.user.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.global.config.TestSecurityConfig;
import com.almang.inventory.user.auth.dto.request.LoginRequest;
import com.almang.inventory.user.auth.dto.response.LoginResponse;
import com.almang.inventory.user.auth.service.AuthService;
import com.almang.inventory.user.auth.service.TokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private TokenService tokenService;
    @MockitoBean private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void 로그인에_성공한다() throws Exception {
        // given
        LoginRequest request = new LoginRequest("store_admin", "password");
        LoginResponse response = new LoginResponse("access-token");

        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.LOGIN_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));
    }

    @Test
    void username이_존재하지_않으면_예외가_발생한다() throws Exception {
        // given
        LoginRequest request = new LoginRequest("error-user", "password");

        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 비밀번호가_일치하지_않으면_예외가_발생한다() throws Exception {
        // given
        LoginRequest request = new LoginRequest("store_admin", "wrong-password");

        when(authService.login(any(LoginRequest.class), any(HttpServletResponse.class)))
                .thenThrow(new BaseException(ErrorCode.INVALID_PASSWORD));

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_PASSWORD.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_PASSWORD.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 로그인_요청값_검증에_실패하면_예외가_발생한다() throws Exception {
        // given
        LoginRequest invalidRequest = new LoginRequest("", "");

        // when & then
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 리프레시_토큰으로_액세스_토큰_재발급에_성공한다() throws Exception {
        // given
        String newAccessToken = "new-access-token";

        when(tokenService.reissueAccessToken(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenReturn(newAccessToken);

        // when & then
        mockMvc.perform(post("/api/v1/auth/reissue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value(SuccessMessage.ACCESS_TOKEN_REISSUE_SUCCESS.getMessage()))
                .andExpect(jsonPath("$.data.accessToken").value(newAccessToken));
    }

    @Test
    void 리프레시_토큰이_유효하지_않으면_재발급_요청시_예외가_발생한다() throws Exception {
        // given
        when(tokenService.reissueAccessToken(any(HttpServletRequest.class), any(HttpServletResponse.class)))
                .thenThrow(new BaseException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
