package com.almang.inventory.global.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SuccessMessage {
    // STORE
    CREATE_STORE_SUCCESS("상점 등록 성공"),
    UPDATE_STORE_SUCCESS("상점 정보 수정 성공"),

    // USER
    SIGNUP_SUCCESS("회원가입 성공"),
    LOGIN_SUCCESS("로그인 성공"),
    ACCESS_TOKEN_REISSUE_SUCCESS("액세스 토큰 재발급 성공"),
    CHANGE_PASSWORD_SUCCESS("비밀번호 변경 성공"),
    LOGOUT_SUCCESS("로그아웃 성공"),
    RESET_PASSWORD_SUCCESS("비밀번호 초기화 성공"),
    GET_USER_PROFILE_SUCCESS("사용자 정보 조회 성공"),
    UPDATE_USER_PROFILE_SUCCESS("사용자 프로필 정보 수정 성공"),
    DELETE_USER_SUCCESS("회원 탈퇴 성공"),

    // PRODUCT
    CREATE_PRODUCT_SUCCESS("품목 등록 성공"),
    UPDATE_PRODUCT_SUCCESS("품목 수정 성공"),
    GET_PRODUCT_DETAIL_SUCCESS("품목 상세 조회 성공"),
    GET_PRODUCT_LIST_SUCCESS("품목 목록 조회 성공"),

    // VENDOR
    CREATE_VENDOR_SUCCESS("발주처 등록 성공"),
    UPDATE_VENDOR_SUCCESS("발주처 수정 성공"),
    GET_VENDOR_DETAIL_SUCCESS("발주처 상세 조회 성공"),
    GET_VENDOR_LIST_SUCCESS("발주처 목록 조회 성공"),

    // ORDER
    CREATE_ORDER_TEMPLATE_SUCCESS("발주 템플릿 등록 성공"),
    UPDATE_ORDER_TEMPLATE_SUCCESS("발주 템플릿 수정 성공"),
    ;

    private final String message;
}
