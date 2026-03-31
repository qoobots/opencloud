package com.qoobot.opencloud.monitor.exception;

import lombok.Getter;

/**
 * 监控模块业务异常
 */
@Getter
public class MonitorException extends RuntimeException {

    private final int code;

    public MonitorException(int code, String message) {
        super(message);
        this.code = code;
    }

    public MonitorException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    // ==================== 指标查询相关 (1001-1004) ====================
    public static MonitorException prometheusUnavailable() {
        return new MonitorException(1001, "监控服务暂时不可用，请稍后重试");
    }

    public static MonitorException promqlError(String detail) {
        return new MonitorException(1002, "PromQL 表达式无效：" + detail);
    }

    public static MonitorException queryTimeout() {
        return new MonitorException(1003, "查询超时，请缩小时间范围或优化查询语句");
    }

    public static MonitorException timeRangeExceeded() {
        return new MonitorException(1004, "时间范围不能超过 30 天");
    }

    // ==================== Grafana 相关 (2001-2003) ====================
    public static MonitorException grafanaUnavailable() {
        return new MonitorException(2001, "Grafana 服务暂时不可用");
    }

    public static MonitorException dashboardNotFound() {
        return new MonitorException(2002, "面板不存在或已被删除");
    }

    public static MonitorException grafanaAuthFailed() {
        return new MonitorException(2003, "Grafana 认证失败，请检查服务配置");
    }

    // ==================== 告警规则相关 (3001-3003) ====================
    public static MonitorException ruleNameDuplicate() {
        return new MonitorException(3001, "规则名称已存在");
    }

    public static MonitorException invalidPromql(String detail) {
        return new MonitorException(3002, "PromQL 表达式无效：" + detail);
    }

    public static MonitorException ruleNotFound() {
        return new MonitorException(3003, "告警规则不存在");
    }

    // ==================== 告警记录相关 (4001-4003) ====================
    public static MonitorException recordNotFound() {
        return new MonitorException(4001, "告警记录不存在");
    }

    public static MonitorException alreadyAcked() {
        return new MonitorException(4002, "告警已确认，无需重复操作");
    }

    public static MonitorException invalidStatusTransition() {
        return new MonitorException(4003, "当前状态不允许此操作");
    }

    // ==================== 通知渠道相关 (5001-5002) ====================
    public static MonitorException channelNotFound() {
        return new MonitorException(5001, "通知渠道不存在");
    }

    public static MonitorException channelInUse(int count) {
        return new MonitorException(5002, "该渠道被 " + count + " 条告警规则引用，请先解绑");
    }

    // ==================== Webhook 相关 (6001-6002) ====================
    public static MonitorException webhookSignatureInvalid() {
        return new MonitorException(6001, "Webhook 签名验证失败");
    }

    public static MonitorException webhookPayloadInvalid() {
        return new MonitorException(6002, "Webhook payload 格式非法");
    }

    // ==================== 静默规则相关 (7001-7005) ====================
    public static MonitorException silenceNotFound() {
        return new MonitorException(7001, "告警静默规则不存在");
    }

    public static MonitorException silenceCannotDelete() {
        return new MonitorException(7002, "仅允许删除已过期或已停止的静默规则");
    }

    // ==================== 通用工厂方法 ====================

    /**
     * 通用异常工厂方法（使用字符串错误码，自动映射到 9xxx 范围）
     */
    public static MonitorException of(String errorCode, String message) {
        return new MonitorException(9000, "[" + errorCode + "] " + message);
    }
}
