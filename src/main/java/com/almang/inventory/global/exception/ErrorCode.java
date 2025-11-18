package com.almang.inventory.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),

    // STORE
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "상점을 찾을 수 없습니다."),

    //USER
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 존재하는 회원 아이디입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}
