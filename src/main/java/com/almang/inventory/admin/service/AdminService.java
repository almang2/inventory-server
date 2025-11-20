package com.almang.inventory.admin.service;

import com.almang.inventory.admin.dto.request.CreateStoreRequest;
import com.almang.inventory.admin.dto.response.CreateStoreResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.repository.StoreRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final StoreRepository storeRepository;

    @Transactional
    public CreateStoreResponse createStore(CreateStoreRequest request) {
        log.info("[AdminService] 상점 생성 요청 - name: {}", request.name());
        validateStoreName(request.name());
        validateDefaultCountCheckThreshold(request.defaultCountCheckThreshold());

        Store store = toEntity(request);
        Store saved = storeRepository.save(store);

        log.info("[AdminService] 상점 생성 성공 - storeId: {}", store.getId());
        return CreateStoreResponse.from(saved);
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

    private Store toEntity(CreateStoreRequest request) {
        return Store.builder()
                .name(request.name())
                .isActivate(true)
                .defaultCountCheckThreshold(request.defaultCountCheckThreshold())
                .build();
    }
}
