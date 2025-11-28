package com.almang.inventory.global.exception;

import com.almang.inventory.global.api.ApiResponse;
import com.almang.inventory.global.monitoring.DiscordErrorNotifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final DiscordErrorNotifier discordErrorNotifier;

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(
            BaseException baseException, HttpServletRequest request
    ) {
        log.error("[BaseException]", baseException);
        notifyToDiscord(baseException, request);

        ErrorCode errorCode = baseException.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception, HttpServletRequest request
    ) {
        log.warn("[ValidationException] {}", exception.getMessage());
        notifyToDiscord(exception, request);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception, HttpServletRequest request
    ) {
        log.error("[Exception]", exception);
        notifyToDiscord(exception, request);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorCode));
    }

    private void notifyToDiscord(Exception exception, HttpServletRequest request) {
        String method = (request != null) ? request.getMethod() : null;
        String path = (request != null) ? request.getRequestURI() : null;
        discordErrorNotifier.notifyException(exception, method, path);
    }
}
