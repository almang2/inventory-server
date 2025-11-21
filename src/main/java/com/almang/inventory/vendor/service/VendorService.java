package com.almang.inventory.vendor.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.dto.request.CreateVendorRequest;
import com.almang.inventory.vendor.dto.request.UpdateVendorRequest;
import com.almang.inventory.vendor.dto.response.VendorResponse;
import com.almang.inventory.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    @Transactional
    public VendorResponse createVendor(CreateVendorRequest request, Long userId) {
        User user = findUserById(userId);

        log.info("[VendorService] 발주처 생성 요청 - userId: {}", user.getId());
        Vendor vendor = toEntity(request, user);
        Vendor saved = vendorRepository.save(vendor);

        log.info("[VendorService] 발주처 생성 성공 - vendorId: {}", saved.getId());
        return VendorResponse.from(saved);
    }

    @Transactional
    public VendorResponse updateVendor(Long vendorId, UpdateVendorRequest request, Long userId) {
        User user = findUserById(userId);
        Vendor vendor = findVendorByIdAndValidateAccess(vendorId, user);

        log.info("[VendorService] 발주처 수정 요청 - userId: {}, vendorId: {}", userId, vendor.getId());
        vendor.updateVendorInfo(
                request.name(), request.channel(), request.contactPoint(), request.note(), request.activated()
        );

        log.info("[VendorService] 발주처 수정 성공 - vendorId: {}", vendor.getId());
        return VendorResponse.from(vendor);
    }

    @Transactional(readOnly = true)
    public VendorResponse getVendorDetail(Long vendorId, Long userId) {
        User user = findUserById(userId);
        Vendor vendor = findVendorByIdAndValidateAccess(vendorId, user);

        log.info("[VendorService] 발주처 상세 조회 성공 - vendorId: {}", vendor.getId());
        return VendorResponse.from(vendor);
    }

    @Transactional(readOnly = true)
    public PageResponse<VendorResponse> getVendorList(
            Long userId, Integer page, Integer size, Boolean isActivate, String nameKeyword
    ) {
        User user = findUserById(userId);
        Store store = user.getStore();

        log.info("[VendorService] 발주처 목록 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        PageRequest pageable = PaginationUtil.createPageRequest(page, size, "name");
        Page<Vendor> vendorPage = findVendorsByFilter(store.getId(), isActivate, nameKeyword, pageable);
        Page<VendorResponse> mapped = vendorPage.map(VendorResponse::from);

        log.info("[VendorService] 발주처 목록 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
    }

    private Vendor toEntity(CreateVendorRequest request, User user) {
        return Vendor.builder()
                .store(user.getStore())
                .name(request.name())
                .channel(request.channel())
                .contactPoint(request.contactPoint())
                .note(request.note())
                .activated(true)
                .build();
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private Vendor findVendorByIdAndValidateAccess(Long vendorId, User user) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        if (!vendor.getStore().getId().equals(user.getStore().getId())) {
            throw new BaseException(ErrorCode.VENDOR_ACCESS_DENIED);
        }

        return vendor;
    }

    private Page<Vendor> findVendorsByFilter(
            Long storeId, Boolean isActivate, String nameKeyword, PageRequest pageable
    ) {
        boolean hasName = nameKeyword != null && !nameKeyword.isBlank();

        // 필터 없음
        if (isActivate == null && !hasName) {
            return vendorRepository.findAllByStoreId(storeId, pageable);
        }

        // 활성 여부
        if (isActivate != null && !hasName) {
            return vendorRepository.findAllByStoreIdAndActivatedTrue(storeId, pageable);
        }

        // 이름 검색
        if (isActivate == null) {
            return vendorRepository.findAllByStoreIdAndNameContainingIgnoreCase(
                    storeId, nameKeyword, pageable
            );
        }

        // 활성 여부 + 이름 검색
        return vendorRepository.findAllByStoreIdAndActivatedTrueAndNameContainingIgnoreCase(
                storeId, nameKeyword, pageable
        );
    }
}
