package com.almang.inventory.user.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.dto.request.UpdateUserProfileRequest;
import com.almang.inventory.user.dto.response.UpdateUserProfileResponse;
import com.almang.inventory.user.dto.response.UserProfileResponse;
import com.almang.inventory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = findUserById(userId);

        log.info("[UserService] 사용자 정보 조회 요청 - userId: {}", user.getId());
        Store store = user.getStore();

        log.info("[UserService] 사용자 정보 조회 성공 - userId: {}", user.getId());
        return UserProfileResponse.from(user, store);
    }

    @Transactional
    public UpdateUserProfileResponse updateUserProfile(Long userId, UpdateUserProfileRequest request) {
        User user = findUserById(userId);

        log.info("[UserService] 사용자 정보 수정 요청 - userId: {}", user.getId());
        user.updateProfile(request.name());

        log.info("[UserService] 사용자 정보 수정 성공 - userId: {}", user.getId());
        return new UpdateUserProfileResponse(true);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }
}
