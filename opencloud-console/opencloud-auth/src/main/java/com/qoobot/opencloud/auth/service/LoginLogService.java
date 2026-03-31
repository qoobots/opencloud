package com.qoobot.opencloud.auth.service;

import com.qoobot.opencloud.auth.domain.entity.LoginLog;
import com.qoobot.opencloud.auth.mapper.LoginLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 登录日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogMapper loginLogMapper;

    /**
     * 记录登录成功日志（异步）
     */
    @Async
    public void recordSuccess(String userId, String tenantId, String jti,
                              HttpServletRequest request) {
        record(userId, tenantId, jti, "SUCCESS", null, request);
    }

    /**
     * 记录登录失败日志（异步）
     */
    @Async
    public void recordFailure(String userId, String failReason,
                              HttpServletRequest request) {
        record(userId, null, null, "FAILED", failReason, request);
    }

    /**
     * 记录主动登出（异步），同时更新 logout_time
     */
    @Async
    public void recordLogout(String userId, String jti) {
        try {
            // 将该 jti 对应的登录记录更新 logout_time
            loginLogMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LoginLog>()
                            .eq(LoginLog::getUserId, userId)
                            .eq(LoginLog::getJti, jti)
                            .set(LoginLog::getStatus, "LOGOUT")
                            .set(LoginLog::getLogoutTime, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("登出日志更新失败: userId={}, jti={}", userId, jti, e);
        }
    }

    /**
     * 记录强制下线（异步）
     */
    @Async
    public void recordForceLogout(String targetUserId) {
        try {
            // 将该用户所有未登出的记录标记为 FORCE_LOGOUT
            loginLogMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<LoginLog>()
                            .eq(LoginLog::getUserId, targetUserId)
                            .eq(LoginLog::getStatus, "SUCCESS")
                            .set(LoginLog::getStatus, "FORCE_LOGOUT")
                            .set(LoginLog::getLogoutTime, LocalDateTime.now()));
        } catch (Exception e) {
            log.warn("强制下线日志更新失败: targetUserId={}", targetUserId, e);
        }
    }

    private void record(String userId, String tenantId, String jti,
                        String status, String failReason,
                        HttpServletRequest request) {
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setUserId(userId);
            loginLog.setTenantId(tenantId);
            loginLog.setJti(jti);
            loginLog.setStatus(status);
            loginLog.setFailReason(failReason);
            if (request != null) {
                loginLog.setLoginIp(getClientIp(request));
                loginLog.setUserAgent(request.getHeader("User-Agent"));
            }
            loginLogMapper.insert(loginLog);
        } catch (Exception e) {
            log.warn("登录日志写入失败: userId={}, status={}", userId, status, e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
