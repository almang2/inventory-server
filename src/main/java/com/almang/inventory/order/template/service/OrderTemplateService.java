package com.almang.inventory.order.template.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.order.template.repository.OrderTemplateRepository;
import com.almang.inventory.order.template.domain.OrderTemplate;
import com.almang.inventory.order.template.dto.request.UpdateOrderTemplateRequest;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.user.repository.UserRepository;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTemplateService {

    private final OrderTemplateRepository orderTemplateRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    @Transactional
    public OrderTemplateResponse updateOrderTemplate(
            Long orderTemplateId, UpdateOrderTemplateRequest request, Long userId
    ) {
        User user = findUserById(userId);
        Vendor vendor = findVendorByIdAndValidateAccess(request.vendorId(), user);
        OrderTemplate orderTemplate = findOrderTemplateById(orderTemplateId, vendor);

        log.info("[OrderTemplateService] 발주처 양식 수정 요청 - userId: {}, orderTemplateId: {}", userId, orderTemplateId);
        orderTemplate.updateTemplate(request.title(), request.body(), request.activated());

        log.info("[OrderTemplateService] 발주처 양식 수정 성공 - userId: {}, orderTemplateId: {}", userId, orderTemplateId);
        return OrderTemplateResponse.from(orderTemplate);
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

    private OrderTemplate findOrderTemplateById(Long orderTemplateId, Vendor vendor) {
        OrderTemplate orderTemplate =  orderTemplateRepository.findById(orderTemplateId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_TEMPLATE_NOT_FOUND));

        if (!orderTemplate.getVendor().getId().equals(vendor.getId())) {
            throw new BaseException(ErrorCode.ORDER_TEMPLATE_ACCESS_DENIED);
        }

        return orderTemplate;
    }
}
