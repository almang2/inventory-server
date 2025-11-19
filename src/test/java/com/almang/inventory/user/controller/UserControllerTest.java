package com.almang.inventory.user.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.almang.inventory.global.api.SuccessMessage;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.security.principal.CustomUserPrincipal;
import com.almang.inventory.store.global.config.TestSecurityConfig;
import com.almang.inventory.user.domain.UserRole;
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

    @Test
    void 사용자_정보_조회에_성공한다() throws Exception {
        // given
        Long userId = 1L;
        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, "store_admin", List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

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
                        .with(authentication(auth))
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
        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, "store_admin", List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(userService.getUserProfile(anyLong()))
                .thenThrow(new BaseException(ErrorCode.USER_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/api/v1/users/me")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ErrorCode.USER_NOT_FOUND.getHttpStatus().value()))
                .andExpect(jsonPath("$.message").value(ErrorCode.USER_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
