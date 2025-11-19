package com.almang.inventory.user.auth.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.user.auth.dto.request.ChangePasswordRequest;
import com.almang.inventory.user.auth.dto.request.LoginRequest;
import com.almang.inventory.user.auth.dto.request.ResetPasswordRequest;
import com.almang.inventory.user.auth.dto.response.ChangePasswordResponse;
import com.almang.inventory.user.auth.dto.response.LoginResponse;
import com.almang.inventory.user.auth.dto.response.LogoutResponse;
import com.almang.inventory.user.auth.dto.response.ResetPasswordResponse;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int PASSWORD_LENGTH = 12;

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletResponse response) {
        User user = findUserByUsername(request.username());
        validatePassword(request.password(), user.getPassword());

        log.info("[AuthService] 로그인 요청 - userId: {}", user.getId());
        String accessToken = issueTokens(user.getId(), response);

        log.info("[AuthService] 로그인 성공 - userId: {}", user.getId());
        return new LoginResponse(accessToken);
    }

    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request, Long userId) {
        User user = findUserById(userId);

        log.info("[AuthService] 비밀번호 변경 요청 - userId: {}", user.getId());
        String encodedPassword = passwordEncoder.encode(request.password());
        user.changePassword(encodedPassword);

        log.info("[AuthService] 비밀번호 변경 성공 - userId: {}", user.getId());
        return new ChangePasswordResponse(true);
    }

    @Transactional
    public LogoutResponse logout(Long userId, HttpServletRequest request, HttpServletResponse response) {
        log.info("[AuthService] 로그아웃 요청 - userId: {}", userId);
        tokenService.revokeTokens(request, response, userId);

        log.info("[AuthService] 로그아웃 성공 - userId: {}", userId);
        return new LogoutResponse(true);
    }

    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        User user = findUserByUsername(request.username());

        log.info("[AuthService] 비밀번호 초기화 요청 - userId: {}", user.getId());
        String randomPassword = generateRandomPassword();
        user.changePassword(passwordEncoder.encode(randomPassword));

        log.info("[AuthService] 비밀번호 초기화 성공 - userId: {}", user.getId());
        return new ResetPasswordResponse(randomPassword);
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private void validatePassword(String password, String encodedPassword) {
        if (!passwordEncoder.matches(password, encodedPassword)) {
            throw new BaseException(ErrorCode.INVALID_PASSWORD);
        }
    }

    private String issueTokens(Long userId, HttpServletResponse response) {
        tokenService.issueRefreshToken(userId, response);
        return tokenService.issueAccessToken(userId);
    }

    private String generateRandomPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);

        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            int index = random.nextInt(PASSWORD_CHARACTERS.length());
            sb.append(PASSWORD_CHARACTERS.charAt(index));
        }
        return sb.toString();
    }
}
