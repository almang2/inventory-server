package com.almang.inventory.cafe24.service;

import com.almang.inventory.customerorder.domain.CustomerOrder;
import com.almang.inventory.customerorder.domain.CustomerOrderItem;
import com.almang.inventory.customerorder.repository.CustomerOrderRepository;
import com.almang.inventory.inventory.repository.InventoryRepository;
import com.almang.inventory.product.domain.Product;
import com.almang.inventory.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class Cafe24OrderService {

    private final ProductRepository productRepository;
    private final com.almang.inventory.store.repository.StoreRepository storeRepository;
    private final com.almang.inventory.vendor.repository.VendorRepository vendorRepository;
    private final com.almang.inventory.customerorder.service.CustomerOrderService customerOrderService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<CustomerOrder> processBeforeDepositOrders(String jsonResponse) {
        List<CustomerOrder> processedOrders = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode ordersNode = rootNode.path("orders");

            if (ordersNode.isArray()) {
                for (JsonNode orderNode : ordersNode) {
                    String cafe24OrderId = orderNode.path("order_id").asText();

                    // 이미 저장된 주문인지 확인
                    Optional<CustomerOrder> existingOrderOpt = customerOrderService.findByCafe24OrderId(cafe24OrderId);
                    if (existingOrderOpt.isPresent()) {
                        processedOrders.add(existingOrderOpt.get());
                        continue;
                    }

                    // 주문 파싱 (저장하지 않음)
                    CustomerOrder customerOrder = parseOrder(orderNode);

                    // 주문 저장 및 재고 업데이트 (CustomerOrderService 위임)
                    CustomerOrder savedOrder = customerOrderService.registerNewOrder(customerOrder);
                    processedOrders.add(savedOrder);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process N00 orders", e);
            throw new RuntimeException("Failed to process N00 orders", e);
        }

        return processedOrders;
    }

    @Transactional
    public List<CustomerOrder> processPreparingShipmentOrders(String jsonResponse) {
        List<CustomerOrder> processedOrders = new ArrayList<>();

        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode ordersNode = rootNode.path("orders");

            if (ordersNode.isArray()) {
                for (JsonNode orderNode : ordersNode) {
                    String cafe24OrderId = orderNode.path("order_id").asText();
                    Optional<CustomerOrder> existingOrderOpt = customerOrderService.findByCafe24OrderId(cafe24OrderId);

                    if (existingOrderOpt.isPresent()) {
                        // 기존 주문이 있는 경우 (N00 -> N10)
                        CustomerOrder existingOrder = existingOrderOpt.get();

                        // 이미 결제 완료된 상태라면 스킵 (중복 처리 방지)
                        if (existingOrder.isPaid()) {
                            continue;
                        }

                        // 결제 완료 처리 및 재고 업데이트 (CustomerOrderService 위임)
                        customerOrderService.processPaymentCompletion(existingOrder, true);
                        processedOrders.add(existingOrder);

                    } else {
                        // 새로운 주문인 경우 (바로 N10)
                        CustomerOrder newOrder = parseOrder(orderNode);

                        // 결제 완료 상태로 설정 (parseOrder 내부에서 처리되거나 여기서 업데이트)
                        if (!newOrder.isPaid()) {
                            newOrder.updatePaidStatus(true);
                        }

                        // 주문 저장 (registerNewOrder를 쓰면 outgoing이 증가하므로, 여기서는 바로 결제 완료 처리 로직을 타야 함)
                        // 하지만 registerNewOrder는 outgoing을 증가시킴.
                        // 바로 N10인 경우: save -> decreaseWarehouse. (outgoing 증가/감소 없음)
                        // CustomerOrderService에 registerPaidOrder 같은게 필요하거나,
                        // registerNewOrder -> processPaymentCompletion(false) 호출.

                        // 1. 저장 (outgoing 증가)
                        CustomerOrder savedOrder = customerOrderService.registerNewOrder(newOrder);

                        // 2. 결제 완료 처리 (outgoing 감소, warehouse 감소)
                        // wasPreviouslyRegistered=true로 하면 outgoing 감소됨.
                        // 결과적으로 outgoing 증가 -> 감소 (상쇄), warehouse 감소.
                        customerOrderService.processPaymentCompletion(savedOrder, true);

                        processedOrders.add(savedOrder);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to process N10 orders", e);
            throw new RuntimeException("Failed to process N10 orders", e);
        }

        return processedOrders;
    }

    private CustomerOrder parseOrder(JsonNode orderNode) {
        // 주문 기본 정보 매핑
        CustomerOrder customerOrder = mapToCustomerOrder(orderNode);

        // 주문 상품 매핑
        JsonNode itemsNode = orderNode.path("items");
        if (itemsNode.isArray()) {
            for (JsonNode itemNode : itemsNode) {
                String variantCode = itemNode.path("variant_code").asText();
                String productCode = itemNode.path("product_code").asText();

                // 상품 조회 (variant_code 우선, 없으면 product_code 시도)
                Optional<Product> productOpt = productRepository.findByCode(variantCode);
                if (productOpt.isEmpty()) {
                    productOpt = productRepository.findByCode(productCode);
                }

                Product product;
                if (productOpt.isPresent()) {
                    product = productOpt.get();
                } else {
                    // 상품이 없으면 생성
                    log.info("Product not found for code: {} / {}. Creating new product.", variantCode, productCode);
                    product = createNewProduct(itemNode, variantCode, productCode);
                }

                CustomerOrderItem orderItem = mapToCustomerOrderItem(itemNode, product);
                customerOrder.addOrderItem(orderItem);
            }
        }

        return customerOrder;
    }

    private Product createNewProduct(JsonNode itemNode, String variantCode, String productCode) {
        // 1. Store 조회 또는 생성
        com.almang.inventory.store.domain.Store newStore = storeRepository.findAll().stream().findFirst()
                .orElseGet(() -> storeRepository.save(com.almang.inventory.store.domain.Store.builder()
                        .name("Default Store")
                        .isActivate(true)
                        .defaultCountCheckThreshold(new BigDecimal("0.20"))
                        .build()));

        // 2. Vendor 조회 또는 생성
        com.almang.inventory.vendor.domain.Vendor vendor = vendorRepository.findAll().stream().findFirst()
                .orElseGet(() -> vendorRepository.save(com.almang.inventory.vendor.domain.Vendor.builder()
                        .store(newStore)
                        .name("Default Vendor")
                        .channel(com.almang.inventory.vendor.domain.VendorChannel.WEB)
                        .contactPoint("000-0000-0000")
                        .activated(true)
                        .build()));

        // 3. Product 생성
        String code = (variantCode != null && !variantCode.isEmpty()) ? variantCode : productCode;
        String name = itemNode.path("product_name").asText("Unknown Product");
        BigDecimal price = new BigDecimal(itemNode.path("product_price").asText("0"));

        return productRepository.save(Product.builder()
                .store(newStore)
                .vendor(vendor)
                .name(name)
                .code(code)
                .unit(com.almang.inventory.product.domain.ProductUnit.EA) // 기본 단위
                .activated(true)
                .retailPrice(price.intValue())
                .costPrice(0) // 원가는 알 수 없음
                .wholesalePrice(0)
                .build());
    }

    private CustomerOrder mapToCustomerOrder(JsonNode orderNode) {
        String orderDateStr = orderNode.path("order_date").asText();
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(orderDateStr);

        JsonNode initialAmountNode = orderNode.path("initial_order_amount");

        return CustomerOrder.builder()
                .cafe24OrderId(orderNode.path("order_id").asText())
                .orderAt(zonedDateTime.toLocalDateTime())
                .isPaid("T".equals(orderNode.path("paid").asText()))
                .isCanceled("T".equals(orderNode.path("canceled").asText()))
                .paymentMethod(orderNode.path("payment_method_name").isArray()
                        ? orderNode.path("payment_method_name").get(0).asText()
                        : "")
                .paymentAmount(new BigDecimal(orderNode.path("payment_amount").asText("0")))
                .billingName(orderNode.path("billing_name").asText())
                .memberId(orderNode.path("member_id").asText())
                .memberEmail(orderNode.path("member_email").asText())
                .initialOrderPriceAmount(new BigDecimal(initialAmountNode.path("order_price_amount").asText("0")))
                .shippingFee(new BigDecimal(initialAmountNode.path("shipping_fee").asText("0")))
                .items(new ArrayList<>())
                .build();
    }

    private CustomerOrderItem mapToCustomerOrderItem(JsonNode itemNode, Product product) {
        return CustomerOrderItem.builder()
                .product(product)
                .productCode(product.getCode()) // Use Product entity's code
                .productName(itemNode.path("product_name").asText())
                .quantity(itemNode.path("quantity").asInt())
                .optionValue(itemNode.path("option_value").asText())
                .variantCode(itemNode.path("variant_code").asText())
                .itemCode(itemNode.path("order_item_code").asText()) // Map order_item_code to itemCode
                .build();
    }
}
