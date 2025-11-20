package com.almang.inventory.global.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuccessMessage {
    // STORE
    CREATE_STORE_SUCCESS("상점 등록 성공"),

    // USER
    SIGNUP_SUCCESS("회원가입 성공"),
    LOGIN_SUCCESS("로그인 성공"),
    ACCESS_TOKEN_REISSUE_SUCCESS("액세스 토큰 재발급 성공"),
    CHANGE_PASSWORD_SUCCESS("비밀번호 변경 성공"),
    LOGOUT_SUCCESS("로그아웃 성공"),
    RESET_PASSWORD_SUCCESS("비밀번호 초기화 성공"),
    GET_USER_PROFILE_SUCCESS("사용자 정보 조회 성공"),
    UPDATE_USER_PROFILE_SUCCESS("사용자 프로필 정보 수정 성공"),
    DELETE_USER_SUCCESS("회원 탈퇴 성공")
    ;

    private final String message;
}
