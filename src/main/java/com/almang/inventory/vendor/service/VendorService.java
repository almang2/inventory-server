package com.almang.inventory.vendor.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.context.UserContextProvider;
import com.almang.inventory.global.context.UserContextProvider.UserStoreContext;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.order.template.repository.OrderTemplateRepository;
import com.almang.inventory.order.template.domain.OrderTemplate;
import com.almang.inventory.order.template.dto.response.OrderTemplateResponse;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.vendor.domain.Vendor;
import com.almang.inventory.vendor.dto.request.CreateOrderTemplateRequest;
import com.almang.inventory.vendor.dto.request.CreateVendorRequest;
import com.almang.inventory.vendor.dto.request.UpdateVendorRequest;
import com.almang.inventory.vendor.dto.response.DeleteVendorResponse;
import com.almang.inventory.vendor.dto.response.VendorResponse;
import com.almang.inventory.vendor.repository.VendorRepository;
import java.util.List;
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
    private final OrderTemplateRepository orderTemplateRepository;
    private final UserContextProvider userContextProvider;
    private final ProductRepository productRepository;

    @Transactional
    public VendorResponse createVendor(CreateVendorRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();

        log.info("[VendorService] 발주처 생성 요청 - userId: {}", user.getId());
        Vendor vendor = toVendorEntity(request, user);
        Vendor saved = vendorRepository.save(vendor);

        log.info("[VendorService] 발주처 생성 성공 - vendorId: {}", saved.getId());
        return VendorResponse.from(saved);
    }

    @Transactional
    public VendorResponse updateVendor(Long vendorId, UpdateVendorRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();
        Vendor vendor = findVendorByIdAndValidateAccess(vendorId, user);

        log.info("[VendorService] 발주처 수정 요청 - userId: {}, vendorId: {}", userId, vendor.getId());
        vendor.updateBasicInfo(request.name(), request.orderMethod());
        vendor.updateContactInfo(request.channel(), request.phoneNumber(), request.email(), request.webPage());
        vendor.updateMeta(request.note(), request.activated());

        log.info("[VendorService] 발주처 수정 성공 - vendorId: {}", vendor.getId());
        return VendorResponse.from(vendor);
    }

    @Transactional
    public DeleteVendorResponse deleteVendor(Long vendorId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();
        Vendor vendor = findVendorByIdAndValidateAccess(vendorId, user);

        log.info("[VendorService] 발주처 삭제 요청 - userId: {}, vendorId: {}", userId, vendor.getId());
        if (productRepository.existsByVendorId(vendor.getId())) {
            throw new BaseException(ErrorCode.VENDOR_HAS_PRODUCTS);
        }
        vendorRepository.delete(vendor);

        log.info("[VendorService] 발주처 삭제 성공 - vendorId: {}", vendor.getId());
        return new DeleteVendorResponse(true);
    }

    @Transactional(readOnly = true)
    public VendorResponse getVendorDetail(Long vendorId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();
        Vendor vendor = findVendorByIdAndValidateAccess(vendorId, user);

        log.info("[VendorService] 발주처 상세 조회 성공 - vendorId: {}", vendor.getId());
        return VendorResponse.from(vendor);
    }

    @Transactional(readOnly = true)
    public PageResponse<VendorResponse> getVendorList(
            Long userId, Integer page, Integer size, Boolean isActivate, String nameKeyword
    ) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[VendorService] 발주처 목록 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        PageRequest pageable = PaginationUtil.createPageRequest(page, size, "name");
        Page<Vendor> vendorPage = findVendorsByFilter(store.getId(), isActivate, nameKeyword, pageable);
        Page<VendorResponse> mapped = vendorPage.map(VendorResponse::from);

        log.info("[VendorService] 발주처 목록 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
    }

    @Transactional
    public OrderTemplateResponse createOrderTemplate(Long vendorId, CreateOrderTemplateRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();
        Vendor vendor = findVendorByIdAndValidateAccess(vendorId, user);

        log.info("[VendorService] 발주처 양식 등록 요청 - userId: {}, vendorId: {}", userId, vendorId);
        OrderTemplate orderTemplate = toOrderTemplateEntity(request, vendor);
        OrderTemplate saved = orderTemplateRepository.save(orderTemplate);

        log.info("[VendorService] 발주처 양식 등록 성공 - orderTemplateId: {}", saved.getId());
        return OrderTemplateResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderTemplateResponse> getOrderTemplates(Long vendorId, Long userId, Boolean activated) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();
        Vendor vendor = findVendorByIdAndValidateAccess(vendorId, user);

        log.info("[VendorService] 발주처 발주처 템플릿 조회 요청 - userId: {}, vendorId: {}", userId, vendorId);
        List<OrderTemplate> templates = findOrderTemplatesByFilter(vendor.getId(), activated);

        log.info("[VendorService] 발주처 발주처 템플릿 조회 성공 - userId: {}, vendorId: {}", userId, vendorId);
        return templates.stream()
                .map(OrderTemplateResponse::from)
                .toList();
    }

    private Vendor toVendorEntity(CreateVendorRequest request, User user) {
        return Vendor.builder()
                .store(user.getStore())
                .name(request.name())
                .channel(request.channel())
                .phoneNumber(request.phoneNumber())
                .email(request.email())
                .webPage(request.webPage())
                .orderMethod(request.orderMethod())
                .note(request.note())
                .activated(true)
                .deletedAt(null)
                .build();
    }

    private OrderTemplate toOrderTemplateEntity(CreateOrderTemplateRequest request, Vendor vendor) {
        return OrderTemplate.builder()
                .vendor(vendor)
                .title(request.title())
                .body(request.body())
                .activated(true)
                .build();
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
        boolean filterActivate = isActivate != null;

        // 1) 필터 없음
        if (!filterActivate && !hasName) {
            return vendorRepository.findAllByStoreId(storeId, pageable);
        }

        // 2) 활성/비활성 필터
        if (filterActivate && !hasName) {
            if (isActivate) {
                return vendorRepository.findAllByStoreIdAndActivatedTrue(storeId, pageable);
            }
            return vendorRepository.findAllByStoreIdAndActivatedFalse(storeId, pageable);
        }

        // 3) 이름 필터
        if (!filterActivate) {
            return vendorRepository.findAllByStoreIdAndNameContainingIgnoreCase(
                    storeId, nameKeyword, pageable
            );
        }

        // 4) 활성 여부 + 이름 필터 둘 다 적용
        if (isActivate) {
            return vendorRepository.findAllByStoreIdAndActivatedTrueAndNameContainingIgnoreCase(
                    storeId, nameKeyword, pageable
            );
        }
        return vendorRepository.findAllByStoreIdAndActivatedFalseAndNameContainingIgnoreCase(
                storeId, nameKeyword, pageable
        );
    }

    private List<OrderTemplate> findOrderTemplatesByFilter(Long vendorId, Boolean activated) {
        if (activated == null) {
            return orderTemplateRepository.findAllByVendorId(vendorId);
        }

        if (Boolean.TRUE.equals(activated)) {
            return orderTemplateRepository.findAllByVendorIdAndActivatedTrue(vendorId);
        }

        return orderTemplateRepository.findAllByVendorIdAndActivatedFalse(vendorId);
    }
}
