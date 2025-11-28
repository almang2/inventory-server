package com.almang.inventory.global.util;

import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskingUtil {

    // Authorization: Bearer abcdef...
    private static final Pattern AUTH_BEARER_PATTERN =
            Pattern.compile("(?i)Authorization:\\s*Bearer\\s+[^\\s]+");

    // access_token=xxx, refresh_token=xxx, password=xxx 형태
    private static final Pattern TOKEN_PARAM_PATTERN =
            Pattern.compile("(?i)(access_token|refresh_token|password)\\s*=\\s*[^&\\s]+");

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

    // 로그/스택트레이스 등 자유 텍스트에서 민감정보를 마스킹
    public static String maskText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String masked = text;
        masked = AUTH_BEARER_PATTERN.matcher(masked).replaceAll("Authorization: Bearer [REDACTED]");
        masked = TOKEN_PARAM_PATTERN.matcher(masked).replaceAll("$1=[REDACTED]");
        return masked;
    }
}
