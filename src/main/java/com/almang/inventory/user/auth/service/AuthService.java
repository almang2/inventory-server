package com.almang.inventory.user.auth.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.user.auth.dto.request.ChangePasswordRequest;
import com.almang.inventory.user.auth.dto.request.LoginRequest;
import com.almang.inventory.user.auth.dto.response.ChangePasswordResponse;
import com.almang.inventory.user.auth.dto.response.LoginResponse;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

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
        user.changePassword(request.password());

        log.info("[AuthService] 비밀번호 변경 성공 - userId: {}", user.getId());
        return new ChangePasswordResponse(true);
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
}
