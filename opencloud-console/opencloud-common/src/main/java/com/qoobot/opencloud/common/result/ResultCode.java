package com.qoobot.opencloud.common.result;

import lombok.Getter;

/**
 * 业务状态码枚举
 */
@Getter
public enum ResultCode {

    // ─── 通用 ──────────────────────────────────────────────────
    SUCCESS(200, "操作成功"),
    INTERNAL_ERROR(500, "服务器内部错误"),
    PARAM_ERROR(400, "请求参数错误"),
    NOT_FOUND(404, "资源不存在"),

    // ─── 认证授权 ────────────────────────────────────────────────
    UNAUTHORIZED(401, "未登录或 Token 已过期"),
    FORBIDDEN(403, "权限不足，拒绝访问"),
    TOKEN_INVALID(4001, "Token 无效"),
    TOKEN_EXPIRED(4002, "Token 已过期"),
    USER_DISABLED(4003, "账号已被禁用"),
    USERNAME_OR_PASSWORD_ERROR(4004, "用户名或密码错误"),

    // ─── 用户管理 ────────────────────────────────────────────────
    USER_NOT_FOUND(5001, "用户不存在"),
    USER_ALREADY_EXISTS(5002, "用户名已存在"),
    OLD_PASSWORD_ERROR(5003, "原密码错误"),

    // ─── 云平台对接 ──────────────────────────────────────────────
    CLOUD_CONNECT_ERROR(6001, "云平台连接失败"),
    CLOUD_API_ERROR(6002, "云平台 API 调用失败"),
    CLUSTER_CONFIG_NOT_FOUND(6003, "集群配置不存在"),

    // ─── 告警 ────────────────────────────────────────────────────
    ALERT_RULE_NOT_FOUND(7001, "告警规则不存在"),
    NOTIFY_CHANNEL_ERROR(7002, "通知渠道发送失败");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
