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
        String method = (request != null) ? request.getMethod() : null;
        String path = (request != null) ? request.getRequestURI() : null;
        discordErrorNotifier.notifyException(baseException, method, path);

        ErrorCode errorCode = baseException.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request
    ) {
        log.warn("[ValidationException] {}", e.getMessage());
        String method = (request != null) ? request.getMethod() : null;
        String path = (request != null) ? request.getRequestURI() : null;
        discordErrorNotifier.notifyException(e, method, path);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception, HttpServletRequest request
    ) {
        log.error("[Exception]", exception);
        String method = (request != null) ? request.getMethod() : null;
        String path = (request != null) ? request.getRequestURI() : null;
        discordErrorNotifier.notifyException(exception, method, path);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.fail(errorCode));
    }
}
