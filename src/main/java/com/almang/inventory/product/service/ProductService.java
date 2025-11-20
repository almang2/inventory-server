package com.almang.inventory.product.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.response.ProductResponse;
import com.almang.inventory.product.repository.ProductRepository;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, Long userId) {
        User user = findUserById(userId);

        log.info("[ProductService] 품목 생성 요청 - userId: {}", user.getId());
        Product product = toEntity(request, user);
        Product saved = productRepository.save(product);

        log.info("[ProductService] 품목 생성 성공 - productId: {}", saved.getId());
        return ProductResponse.from(saved);
    }

    private Product toEntity(CreateProductRequest request, User user) {
        Vendor vendor = findVendorById(request.vendorId());

        if (!vendor.getStore().getId().equals(user.getStore().getId())) {
            throw new BaseException(ErrorCode.VENDOR_ACCESS_DENIED);
        }

        return Product.builder()
                .store(user.getStore())
                .vendor(vendor)
                .name(request.name())
                .code(request.code())
                .unit(request.unit())
                .boxWeightG(request.boxWeightG())
                .unitPerBox(request.unitPerBox())
                .unitWeightG(request.unitWeightG())
                .isActivate(true)
                .costPrice(request.costPrice())
                .retailPrice(request.retailPrice())
                .wholesalePrice(request.wholesalePrice())
                .build();
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private Vendor findVendorById(Long id) {
        return vendorRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.VENDOR_NOT_FOUND));
    }
}
