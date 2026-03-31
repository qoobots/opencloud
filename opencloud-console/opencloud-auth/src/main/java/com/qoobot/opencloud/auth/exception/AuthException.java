package com.qoobot.opencloud.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Auth 模块业务异常
 * - errorCode 对应应用设计中的 AUTH_XXXX 规范
 * - httpStatus 控制 HTTP 响应状态码
 */
@Getter
public class AuthException extends RuntimeException {

    private final String errorCode;
    private final int    httpStatus;

    /** 默认 HTTP 400 */
    public AuthException(String errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public AuthException(String errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode  = errorCode;
        this.httpStatus = status.value();
    }

    // ── 快捷工厂方法 ──────────────────────────────────────────

    public static AuthException captchaExpired() {
        return new AuthException("AUTH_0005", "验证码已过期，请重新获取");
    }

    public static AuthException captchaError() {
        return new AuthException("AUTH_0004", "验证码错误");
    }

    public static AuthException badCredentials() {
        return new AuthException("AUTH_0001", "账号或密码错误");
    }

    public static AuthException accountDisabled() {
        return new AuthException("AUTH_0002", "账号已被禁用，请联系管理员");
    }

    public static AuthException accountLocked(long remainMinutes) {
        return new AuthException("AUTH_0003",
                "账号已锁定，请 " + remainMinutes + " 分钟后重试");
    }

    public static AuthException loginRateLimit() {
        return new AuthException("AUTH_0006", "操作过于频繁，请稍后再试",
                HttpStatus.TOO_MANY_REQUESTS);
    }

    public static AuthException tokenMissing() {
        return new AuthException("AUTH_1001", "未授权，请登录",
                HttpStatus.UNAUTHORIZED);
    }

    public static AuthException tokenInvalid() {
        return new AuthException("AUTH_1002", "Token 无效（签名非法）",
                HttpStatus.UNAUTHORIZED);
    }

    public static AuthException tokenExpired() {
        return new AuthException("AUTH_1003", "Token 已过期，请使用 RefreshToken 刷新",
                HttpStatus.UNAUTHORIZED);
    }

    public static AuthException tokenRevoked() {
        return new AuthException("AUTH_1004", "Token 已注销，请重新登录",
                HttpStatus.UNAUTHORIZED);
    }

    public static AuthException tokenVersionMismatch() {
        return new AuthException("AUTH_1005", "登录状态已失效，请重新登录",
                HttpStatus.UNAUTHORIZED);
    }

    public static AuthException refreshTokenInvalid() {
        return new AuthException("AUTH_1006", "登录已过期，请重新登录",
                HttpStatus.UNAUTHORIZED);
    }

    public static AuthException refreshTokenReplay() {
        return new AuthException("AUTH_1007", "安全风险，请重新登录",
                HttpStatus.UNAUTHORIZED);
    }

    public static AuthException oldPasswordError() {
        return new AuthException("AUTH_3001", "旧密码不正确");
    }

    public static AuthException weakPassword() {
        return new AuthException("AUTH_3002", "密码须至少 8 位，包含大小写字母和数字");
    }

    public static AuthException sameAsOldPassword() {
        return new AuthException("AUTH_3003", "新密码不能与旧密码相同");
    }

    public static AuthException passwordNotMatch() {
        return new AuthException("AUTH_3004", "两次输入密码不一致");
    }

    public static AuthException userNotFound() {
        return new AuthException("AUTH_4001", "用户不存在", HttpStatus.NOT_FOUND);
    }

    public static AuthException accountNotLocked() {
        return new AuthException("AUTH_4002", "账号当前未处于锁定状态");
    }
}
