package com.qoobot.opencloud.cloud.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 云平台模块业务异常
 */
@Getter
public class CloudException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public CloudException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public CloudException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = status.value();
    }
}
