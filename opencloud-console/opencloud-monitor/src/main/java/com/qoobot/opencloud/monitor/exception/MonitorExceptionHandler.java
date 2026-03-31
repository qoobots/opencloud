package com.qoobot.opencloud.monitor.exception;

import com.qoobot.opencloud.common.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 监控模块异常处理器
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.qoobot.opencloud.monitor")
@Order(2)
public class MonitorExceptionHandler {

    @ExceptionHandler(MonitorException.class)
    public R<Void> handleMonitorException(MonitorException e) {
        log.warn("Monitor业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }
}
