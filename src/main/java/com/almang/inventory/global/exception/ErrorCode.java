package com.almang.inventory.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "유효하지 않은 요청 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    // STORE
    STORE_NAME_IS_LONG(HttpStatus.BAD_REQUEST, "상점 이름은 20자를 초과할 수 없습니다."),
    DEFAULT_COUNT_CHECK_THRESHOLD_NOT_IN_RANGE(HttpStatus.BAD_REQUEST, "기본 임계치는 0과 1 사이여야 합니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "상점을 찾을 수 없습니다."),
    STORE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "상점에 접근할 수 없습니다."),

    //USER
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 회원 아이디입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 잘못되었습니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "Refresh Token이 존재하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다."),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 Access Token 입니다."),
    ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access Token 입니다."),
    NAME_IS_LONG(HttpStatus.BAD_REQUEST, "이름은 20자를 초과할 수 없습니다"),

    // VENDOR
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "발주처를 찾을 수 없습니다."),
    VENDOR_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 상점의 발주처가 아닙니다."),
    VENDOR_HAS_PRODUCTS(HttpStatus.CONFLICT, "해당 발주처를 사용하는 품목이 존재하여 삭제할 수 없습니다."),

    // PRODUCT
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND,"품목을 찾을 수 없습니다."),
    PRODUCT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 상점의 상품이 아닙니다."),

    // ORDER
    ORDER_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "발주 템플릿을 찾을 수 없습니다."),
    ORDER_TEMPLATE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 상점의 발주 템플릿이 아닙니다."),
    ORDER_ITEM_EMPTY(HttpStatus.BAD_REQUEST, "주문 목록이 비어있습니다."),
    ORDER_ITEM_MUST_HAVE_ORDER_AND_PRODUCT(HttpStatus.BAD_REQUEST, "발주 상세는 발주와 품목 모두 가져야 합니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "발주를 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 상점의 발주가 아닙니다."),
    VENDOR_CHANGE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "해당 발주를 변경할 수 없습니다."),
    ORDER_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "발주 상품을 찾을 수 없습니다."),
    ORDER_ITEM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 발주의 발주 항목이 아닙니다."),
    ORDER_ALREADY_DELIVERED(HttpStatus.BAD_REQUEST, "이미 도착한 주문입니다."),
    ORDER_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 주문입니다."),

    // RECEIPT
    RECEIPT_CREATION_NOT_ALLOWED_FROM_ORDER(HttpStatus.BAD_REQUEST, "해당 발주 상태에서는 입고를 생성할 수 없습니다."),
    RECEIPT_NOT_FOUND(HttpStatus.NOT_FOUND, "입고를 찾을 수 없습니다."),
    RECEIPT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 상점의 입고가 아닙니다."),
    RECEIPT_ORDER_MISMATCH(HttpStatus.FORBIDDEN, "해당 발주의 입고가 아닙니다."),
    RECEIPT_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "입고 아이템을 찾을 수 없습니다."),
    RECEIPT_ITEM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "입고 아이템에 접근할 수 없습니다."),
    RECEIPT_ALREADY_CANCELED(HttpStatus.BAD_REQUEST, "이미 취소된 입고입니다."),
    RECEIPT_ALREADY_CONFIRMED(HttpStatus.BAD_REQUEST, "이미 확정된 입고입니다."),

    // INVENTORY
    INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "재고를 찾을 수 없습니다."),
    INCOMING_STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "입고 예정 수량이 부족합니다."),
    WAREHOUSE_STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "창고 재고가 부족합니다."),
    DISPLAY_STOCK_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "매대 재고가 부족합니다."),
    INVENTORY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 상점의 재고가 아닙니다."),
    INVENTORY_PRODUCT_MISMATCH(HttpStatus.BAD_REQUEST, "요청한 상품 정보와 재고의 상품 정보가 일치하지 않습니다."),

    // CUSTOMER_ORDER
    DUPLICATE_CUSTOMER_ORDER(HttpStatus.CONFLICT, "이미 존재하는 고객 주문입니다."),
    NOT_ENOUGH_STOCK(HttpStatus.BAD_REQUEST, "요청한 상품의 재고가 부족합니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
