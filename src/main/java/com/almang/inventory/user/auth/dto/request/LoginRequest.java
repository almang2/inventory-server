package com.almang.inventory.user.auth.dto.request;

public record LoginRequest(
        String username,
        String password
) {}
