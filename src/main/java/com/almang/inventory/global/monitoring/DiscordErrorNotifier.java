package com.almang.inventory.global.monitoring;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.almang.inventory.global.logging.InMemoryLogAppender;
import jakarta.servlet.http.HttpServletRequest;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordErrorNotifier {

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

    private static final int MAX_SECTION_LENGTH = 800;
    private static final int RECENT_LOG_LIMIT = 20;
    private static final String TRUNCATED_SUFFIX = "\n...(생략)";
    private static final int DISCORD_CONTENT_LIMIT = 2000;
    private static final String MESSAGE_TRUNCATED_SUFFIX = "\n\n...(메시지가 너무 길어 일부만 표시됩니다)";

    private static final String ERROR_TEMPLATE = """
        📛 *Almang 서버 에러 발생*

        - 경로: `%s %s`
        - 예외 타입: `%s`
        - 메시지: `%s`

        🧾 최근 로그 (최신 20개)
        ```text
        %s
        ```

        🔍 스택트레이스
        ```text
        %s
        ```
        """;

    @Value("${monitoring.discord.error-webhook-url:}")
    private String webhookUrl;

    @Value("${monitoring.discord.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    @Async("discordNotifierExecutor")
    public void notifyException(Throwable exception, String method, String path) {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String safePath = (path != null) ? path : "알 수 없음";
        String safeMethod = (method != null) ? method : "알 수 없음";

        String stackTrace = truncateSection(getStackTrace(exception));
        String recentLogs = truncateSection(formatRecentLogs());

        String content = ERROR_TEMPLATE.formatted(
                safeMethod,
                safePath,
                exception.getClass().getName(),
                safeMessage(exception.getMessage()),
                recentLogs,
                stackTrace
        );

        content = truncateForDiscord(content);
        sendToDiscord(content);
    }

    private String formatRecentLogs() {
        List<ILoggingEvent> events = InMemoryLogAppender.getRecentEvents(RECENT_LOG_LIMIT);

        return events.stream()
                .filter(e -> e.getLevel().isGreaterOrEqual(Level.INFO))
                .map(e -> String.format(
                        "%s %-5s %s - %s",
                        TIME_FORMATTER.format(Instant.ofEpochMilli(e.getTimeStamp())),
                        e.getLevel(),
                        shortLoggerName(e.getLoggerName()),
                        e.getFormattedMessage()
                ))
                .collect(Collectors.joining("\n"));
    }

    private String shortLoggerName(String loggerName) {
        if (loggerName == null) {
            return "";
        }
        if (loggerName.length() > 40) {
            return "..." + loggerName.substring(loggerName.length() - 40);
        }
        return loggerName;
    }

    private void sendToDiscord(String content) {
        try {
            Map<String, Object> payload = Map.of("content", content);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, entity, String.class);
        } catch (Exception e) {
            log.warn("디스코드로 에러 로그 전송에 실패했습니다: {}", e.getMessage());
        }
    }

    private String getStackTrace(Throwable exception) {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    private String truncateSection(String value) {
        if (value == null) {
            return "";
        }
        if (value.length() <= MAX_SECTION_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_SECTION_LENGTH) + TRUNCATED_SUFFIX;
    }

    private String truncateForDiscord(String content) {
        if (content == null) {
            return "";
        }
        if (content.length() <= DISCORD_CONTENT_LIMIT) {
            return content;
        }
        int max = DISCORD_CONTENT_LIMIT - MESSAGE_TRUNCATED_SUFFIX.length();

        return content.substring(0, max) + MESSAGE_TRUNCATED_SUFFIX;
    }

    private String safeMessage(String message) {
        return (message == null || message.isBlank()) ? "메시지 없음" : message;
    }
}
