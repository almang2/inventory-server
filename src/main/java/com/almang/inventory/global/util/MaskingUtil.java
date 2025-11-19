package com.almang.inventory.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskingUtil {

    public static String maskUsername(String username) {
        if (username == null || username.isBlank()) {
            return username;
        }

        // 앞 2글자만 남기고 나머지 *
        if (username.length() <= 2) {
            return "*".repeat(username.length());
        }

        String prefix = username.substring(0, 2);
        String masked = "*".repeat(username.length() - 2);
        return prefix + masked;
    }
}
