package com.qoobot.opencloud.cloud.domain.enums;

import lombok.Getter;

/**
 * 集群状态枚举
 */
@Getter
public enum ClusterStatus {
    ACTIVE("ACTIVE", "正常"),
    ERROR("ERROR", "连接失败"),
    PENDING("PENDING", "待测试");

    private final String code;
    private final String desc;

    ClusterStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
