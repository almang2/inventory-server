package com.almang.inventory.vendor.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.dto.request.CreateVendorRequest;
import com.almang.inventory.vendor.dto.request.UpdateVendorRequest;
import com.almang.inventory.vendor.dto.response.VendorResponse;
import com.almang.inventory.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
