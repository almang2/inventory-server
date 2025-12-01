package com.almang.inventory.product.service;

import com.almang.inventory.global.api.PageResponse;
import com.almang.inventory.global.context.UserContextProvider;
import com.almang.inventory.global.context.UserContextProvider.UserStoreContext;
import com.almang.inventory.global.exception.BaseException;
import com.almang.inventory.global.exception.ErrorCode;
import com.almang.inventory.global.util.PaginationUtil;
import com.almang.inventory.inventory.service.InventoryService;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.dto.request.CreateProductRequest;
import com.almang.inventory.product.dto.request.UpdateProductRequest;
import com.almang.inventory.product.dto.response.DeleteProductResponse;
import com.almang.inventory.product.dto.response.ProductResponse;
import com.almang.inventory.product.repository.ProductRepository;
import com.almang.inventory.store.domain.Store;
import com.almang.inventory.user.domain.User;
import com.almang.inventory.vendor.domain.Vendor;
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
public class ProductService {

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final VendorRepository vendorRepository;
    private final UserContextProvider userContextProvider;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();

        log.info("[ProductService] 품목 생성 요청 - userId: {}", user.getId());
        Product product = toEntity(request, user);
        Product saved = productRepository.save(product);
        inventoryService.createInventory(saved, request.reorderTriggerPoint());

        log.info("[ProductService] 품목 생성 성공 - productId: {}", saved.getId());
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, UpdateProductRequest request, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();
        Product product = findProductById(productId);
        validateStoreAccess(product, user);

        Vendor vendor = findVendorByIdAndValidateAccess(request.vendorId(), user);

        log.info("[ProductService] 품목 수정 요청 - userId: {}, productId: {}", user.getId(), product.getId());

        product.updateVendor(vendor);
        product.updateBasicInfo(request.name(), request.code(), request.unit());
        product.updateWeights(request.boxWeightG(), request.unitPerBox(), request.unitWeightG());
        product.updatePrices(request.costPrice(), request.retailPrice(), request.wholesalePrice());
        product.updateActivation(request.isActivated());

        log.info("[ProductService] 품목 수정 성공 - productId: {}", product.getId());
        return ProductResponse.from(product);
    }

    @Transactional
    public DeleteProductResponse deleteProduct(Long productId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();
        Product product = findProductById(productId);
        validateStoreAccess(product, user);

        log.info("[ProductService] 품목 삭제 요청 - userId: {}, productId: {}", user.getId(), product.getId());
        product.delete();

        log.info("[ProductService] 품목 삭제 성공 - productId: {}", product.getId());
        return new DeleteProductResponse(true);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductDetail(Long productId, Long userId) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        User user = context.user();
        Product product = findProductById(productId);
        validateStoreAccess(product, user);

        log.info("[ProductService] 품목 상세 조회 성공 - productId: {}", product.getId());
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getProductList(
            Long userId, Integer page, Integer size, Boolean isActivate, String nameKeyword
    ) {
        UserStoreContext context = userContextProvider.findUserAndStore(userId);
        Store store = context.store();

        log.info("[ProductService] 품목 목록 조회 요청 - userId: {}, storeId: {}", userId, store.getId());
        PageRequest pageable = PaginationUtil.createPageRequest(page, size, "name");
        Page<Product> productPage = findProductsByFilter(store.getId(), isActivate, nameKeyword, pageable);
        Page<ProductResponse> mapped = productPage.map(ProductResponse::from);

        log.info("[ProductService] 품목 목록 조회 성공 - userId: {}, storeId: {}", userId, store.getId());
        return PageResponse.from(mapped);
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
                .activated(true)
                .costPrice(request.costPrice())
                .retailPrice(request.retailPrice())
                .wholesalePrice(request.wholesalePrice())
                .deletedAt(null)
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

    private Product findProductById(Long id) {
        Product product =  productRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getDeletedAt() != null) {
            throw new BaseException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return product;
    }

    private void validateStoreAccess(Product product, User user) {
        if (!product.getStore().getId().equals(user.getStore().getId())) {
            throw new BaseException(ErrorCode.STORE_ACCESS_DENIED);
        }
    }

    private Page<Product> findProductsByFilter(
            Long storeId, Boolean isActivate, String nameKeyword, PageRequest pageable
    ) {
        boolean hasName = nameKeyword != null && !nameKeyword.isBlank();
        boolean filterActivate = isActivate != null;

        // 1) 필터 없음
        if (!filterActivate && !hasName) {
            return productRepository.findAllByStoreId(storeId, pageable);
        }

        // 2) 활성/비활성 필터
        if (filterActivate && !hasName) {
            if (isActivate) {
                return productRepository.findAllByStoreIdAndActivatedTrue(storeId, pageable);
            }
            return productRepository.findAllByStoreIdAndActivatedFalse(storeId, pageable);
        }

        // 3) 이름 검색
        if (!filterActivate) {
            return productRepository.findAllByStoreIdAndNameContainingIgnoreCase(
                    storeId, nameKeyword, pageable
            );
        }

        // 4) 활성 + 이름 검색
        if (isActivate) {
            return productRepository.findAllByStoreIdAndActivatedTrueAndNameContainingIgnoreCase(
                    storeId, nameKeyword, pageable
            );
        }
        return productRepository.findAllByStoreIdAndActivatedFalseAndNameContainingIgnoreCase(
                storeId, nameKeyword, pageable
        );
    }
}
