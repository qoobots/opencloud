package com.qoobot.opencloud.auth.domain.enums;

import lombok.Getter;

/**
 * 登录状态枚举
 */
@Getter
public enum LoginStatus {

    SUCCESS("SUCCESS", "登录成功"),
    FAILED("FAILED", "登录失败"),
    LOGOUT("LOGOUT", "主动登出"),
    FORCE_LOGOUT("FORCE_LOGOUT", "强制下线");

    private final String code;
    private final String desc;

    LoginStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
