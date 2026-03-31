package com.qoobot.opencloud.system.log.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.system.log.annotation.SysLog;
import com.qoobot.opencloud.system.log.domain.entity.SysOperationLog;
import com.qoobot.opencloud.system.log.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志 AOP 切面
 * 自动采集操作上下文，异步写入 sys_operation_log
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SysLogAspect {

    private final OperationLogService operationLogService;
    private final HttpServletRequest  request;
    private final ObjectMapper        objectMapper;

    @Around("@annotation(sysLog)")
    public Object around(ProceedingJoinPoint pjp, SysLog sysLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysOperationLog opLog = new SysOperationLog();

        // 从 SecurityContext 获取操作人信息（principal = userId String）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof String userId) {
                opLog.setUserId(userId);
                // 从 request attribute 中补充 tenantId / username（由 JwtAuthenticationFilter 写入）
                Object tenantId = request.getAttribute("tenantId");
                if (tenantId instanceof String tid) {
                    opLog.setTenantId(tid);
                }
                opLog.setUsername(auth.getName());
            }
        }

        opLog.setModuleName(sysLog.module());
        opLog.setOperationType(sysLog.action());
        opLog.setMethod(request.getMethod());
        opLog.setRequestUrl(request.getRequestURI());
        opLog.setOperateIp(getClientIp(request));
        opLog.setOperateTime(LocalDateTime.now());

        // 序列化请求参数（脱敏）
        try {
            Object[] args = pjp.getArgs();
            String paramJson = serializeArgs(args);
            opLog.setRequestParam(paramJson.length() > 4096 ? paramJson.substring(0, 4096) + "..." : paramJson);
        } catch (Exception ignored) {}

        try {
            Object result = pjp.proceed();
            opLog.setStatus("SUCCESS");
            try {
                String resultJson = objectMapper.writeValueAsString(result);
                opLog.setResponseResult(resultJson.length() > 2048 ? resultJson.substring(0, 2048) + "..." : resultJson);
            } catch (Exception ignored) {}
            return result;
        } catch (Exception e) {
            opLog.setStatus("FAILED");
            String msg = e.getMessage();
            opLog.setExceptionMsg(msg != null && msg.length() > 1024 ? msg.substring(0, 1024) : msg);
            throw e;
        } finally {
            opLog.setCostTime((int) (System.currentTimeMillis() - startTime));
            operationLogService.saveAsync(opLog);
        }
    }

    private String serializeArgs(Object[] args) {
        try {
            String json = objectMapper.writeValueAsString(args);
            return desensitize(json);
        } catch (Exception e) {
            return "[]";
        }
    }

    private String desensitize(String json) {
        if (json == null) return null;
        return json
                .replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
                .replaceAll("\"newPassword\"\\s*:\\s*\"[^\"]*\"", "\"newPassword\":\"***\"")
                .replaceAll("\"passwordHash\"\\s*:\\s*\"[^\"]*\"", "\"passwordHash\":\"***\"")
                .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"");
    }

    private String getClientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
