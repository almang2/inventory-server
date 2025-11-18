package com.almang.inventory.store.admin.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.admin.dto.request.StoreAdminCreateRequest;
import com.almang.inventory.store.admin.dto.response.StoreAdminCreateResponse;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.domain.UserRole;
import com.almang.inventory.user.repository.UserRepository;
import java.security.SecureRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreAdminService {

    private static final String PASSWORD_CHARACTERS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final int PASSWORD_LENGTH = 12;

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public StoreAdminCreateResponse createStoreAdmin(StoreAdminCreateRequest request) {
        Store store = storeRepository.findById(request.storeId())
                .orElseThrow(() -> new BaseException(ErrorCode.STORE_NOT_FOUND));

        validateUniqueUsername(request.username());

        String randomPassword = generateRandomPassword();
        String encodedPassword = passwordEncoder.encode(randomPassword);

        User user = User.builder()
                .store(store)
                .username(request.username())
                .password(encodedPassword)
                .name(request.name())
                .role(UserRole.ADMIN)
                .build();

        userRepository.save(user);

        return new StoreAdminCreateResponse(
                user.getId(), user.getUsername(), randomPassword, user.getName(), store.getId()
        );
    }

    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new BaseException(ErrorCode.DUPLICATE_USERNAME);
        }
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
