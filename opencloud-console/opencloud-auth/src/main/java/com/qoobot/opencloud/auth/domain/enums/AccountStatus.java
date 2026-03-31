package com.qoobot.opencloud.auth.domain.enums;

import lombok.Getter;

/**
 * 账号状态枚举
 */
@Getter
public enum AccountStatus {

    ACTIVE("ACTIVE", "正常"),
    DISABLED("DISABLED", "禁用"),
    LOCKED("LOCKED", "锁定");

    private final String code;
    private final String desc;

    AccountStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
