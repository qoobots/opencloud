package com.qoobot.opencloud.cloud.domain.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatus {
    PENDING("PENDING", "等待中"),
    RUNNING("RUNNING", "执行中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    TaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
