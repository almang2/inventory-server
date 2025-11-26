package com.almang.inventory.store.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.context.UserContextProvider;
import com.almang.inventory.global.context.UserContextProvider.UserStoreContext;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.order.template.domain.OrderTemplate;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.order.template.repository.OrderTemplateRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.store.dto.request.UpdateStoreRequest;
import com.almang.inventory.store.dto.response.UpdateStoreResponse;
import com.almang.inventory.user.repository.UserRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreService {

    private final OrderTemplateRepository orderTemplateRepository;
    private final UserContextProvider userContextProvider;

    @Transactional
    public UpdateStoreResponse updateStore(UpdateStoreRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

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

    @Transactional(readOnly = true)
    public PageResponse<OrderTemplateResponse> getStoreOrderTemplateList(
            Long userId, Integer page, Integer size, Boolean activated
    ) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[StoreService] 상점 발주 템플릿 목록 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        PageRequest pageable = PaginationUtil.createPageRequest(page, size, "title");
        Page<OrderTemplate> orderTemplatePage = findStoreOrderTemplatesByFilter(store.getId(), activated, pageable);
        Page<OrderTemplateResponse> mapped = orderTemplatePage.map(OrderTemplateResponse::from);

        log.info("[StoreService] 상점 발주 템플릿 목록 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
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

    private Page<OrderTemplate> findStoreOrderTemplatesByFilter(
            Long storeId, Boolean activated, PageRequest pageable
    ) {
        // 활성 필터 없음
        if (activated == null) {
            return orderTemplateRepository.findAllByVendorStoreId(storeId, pageable);
        }

        // 활성 / 비활성 필터
        if (Boolean.TRUE.equals(activated)) {
            return orderTemplateRepository.findAllByVendorStoreIdAndActivatedTrue(storeId, pageable);
        }
        return orderTemplateRepository.findAllByVendorStoreIdAndActivatedFalse(storeId, pageable);
    }
}
