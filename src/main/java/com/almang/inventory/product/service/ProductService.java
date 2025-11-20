package com.almang.inventory.product.service;

import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.request.UpdateProductRequest;
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

    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request, Long userId) {
        User user = findUserById(userId);
        Product product = findProductById(productId);
        validateStoreAccess(product, user);

        Vendor vendor = findVendorByIdAndValidateAccess(request.vendorId(), user);

        log.info("[ProductService] 품목 수정 요청 - userId: {}, productId: {}", user.getId(), product.getId());

        product.updateVendor(vendor);
        product.updateBasicInfo(request.name(), request.code(), request.unit());
        product.updateWeights(request.boxWeightG(), request.unitPerBox(), request.unitWeightG());
        product.updatePrices(request.costPrice(), request.retailPrice(), request.wholesalePrice());
        product.updateActivation(request.isActivate());

        log.info("[ProductService] 품목 수정 성공 - productId: {}", product.getId());
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId, Long userId) {
        User user = findUserById(userId);
        Product product = findProductById(productId);
        validateStoreAccess(product, user);

        log.info("[ProductService] 품목 상세 조회 성공 - productId: {}", product.getId());
        return ProductResponse.from(product);
    }

    private Product toEntity(CreateProductRequest request, User user) {
        Vendor vendor = findVendorByIdAndValidateAccess(request.vendorId(), user);

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

    private Vendor findVendorByIdAndValidateAccess(Long vendorId, User user) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new BaseException(ErrorCode.VENDOR_NOT_FOUND));

        if (!vendor.getStore().getId().equals(user.getStore().getId())) {
            throw new BaseException(ErrorCode.VENDOR_ACCESS_DENIED);
        }

        return vendor;
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void validateStoreAccess(Product product, User user) {
        if (!product.getStore().getId().equals(user.getStore().getId())) {
            throw new BaseException(ErrorCode.STORE_ACCESS_DENIED);
        }
    }
}
