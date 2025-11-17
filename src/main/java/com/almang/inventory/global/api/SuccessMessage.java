package com.almang.inventory.global.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuccessMessage {
    SIGNUP_SUCCESS("회원가입 성공"),
    ;

    private final String message;
}
