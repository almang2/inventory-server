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

    // PRODUCT
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND,"품목을 찾을 수 없습니다."),

    // ORDER
    ORDER_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "발주 템플릿을 찾을 수 없습니다."),
    ORDER_TEMPLATE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "해당 발주처의 발주 템플릿이 아닙니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
