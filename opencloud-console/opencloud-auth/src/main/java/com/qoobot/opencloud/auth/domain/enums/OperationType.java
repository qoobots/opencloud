package com.qoobot.opencloud.auth.domain.enums;

import lombok.Getter;

/**
 * 操作类型枚举
 */
@Getter
public enum OperationType {

    CHANGE_PASSWORD("CHANGE_PASSWORD", "修改密码"),
    FORCE_LOGOUT("FORCE_LOGOUT", "强制下线"),
    UNLOCK_ACCOUNT("UNLOCK_ACCOUNT", "解锁账号");

    private final String code;
    private final String desc;

    OperationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
