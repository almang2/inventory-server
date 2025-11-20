package com.almang.inventory.store.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.dto.request.UpdateStoreRequest;
import com.almang.inventory.store.dto.response.UpdateStoreResponse;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final UserRepository userRepository;

    @Transactional
    public UpdateStoreResponse updateStore(UpdateStoreRequest request, Long userId) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[StoreService] 상점 정보 수정 요청 - userId: {}, storeId: {}", userId, store.getId());

        if (request.name() != null) {
            validateStoreName(request.name());
            store.updateName(request.name());
        }

        if (request.defaultCountCheckThreshold() != null) {
            validateDefaultCountCheckThreshold(request.defaultCountCheckThreshold());
            store.updateThreshold(request.defaultCountCheckThreshold());
        }

        if (request.isActivate() != null) {
            store.updateActivation(request.isActivate());
        }

        log.info("[StoreService] 상점 정보 수정 성공 - storeId: {}", store.getId());
        return UpdateStoreResponse.from(store);
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateStoreName(String storeName) {
        if (storeName.length() > 20) {
            throw new BaseException(ErrorCode.STORE_NAME_IS_LONG);
        }
    }

    private void validateDefaultCountCheckThreshold(BigDecimal defaultCountCheckThreshold) {
        if (defaultCountCheckThreshold.compareTo(BigDecimal.ZERO) < 0
                || defaultCountCheckThreshold.compareTo(BigDecimal.ONE) > 0) {
            throw new BaseException(ErrorCode.DEFAULT_COUNT_CHECK_THRESHOLD_NOT_IN_RANGE);
        }
    }
}
