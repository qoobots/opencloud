package com.qoobot.opencloud.auth.exception;

import com.qoobot.opencloud.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Auth 模块异常处理器（覆盖 common 模块 GlobalExceptionHandler 对 AuthException 的处理）
 */
@Slf4j
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<R<Void>> handleAuthException(AuthException e) {
        log.warn("Auth 业务异常: [{}] {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(R.fail(e.getErrorCode(), e.getMessage()));
    }
}
