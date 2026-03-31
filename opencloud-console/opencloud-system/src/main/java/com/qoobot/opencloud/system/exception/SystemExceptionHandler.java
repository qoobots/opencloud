package com.qoobot.opencloud.system.exception;

import com.qoobot.opencloud.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 系统管理模块异常处理器
 */
@Slf4j
@RestControllerAdvice
@Order(1)
public class SystemExceptionHandler {

    @ExceptionHandler(SystemException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleSystemException(SystemException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(400, "[" + e.getCode() + "] " + e.getMessage());
    }
}
