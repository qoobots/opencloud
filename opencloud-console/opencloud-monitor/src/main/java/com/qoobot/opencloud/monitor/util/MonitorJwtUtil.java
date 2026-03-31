package com.qoobot.opencloud.monitor.util;

import com.qoobot.opencloud.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Monitor 模块 JWT 工具类
 * <p>
 * 对 common 模块 {@link JwtUtil} 的封装，添加 monitor 场景需要的 userId/tenantId 提取方法。
 * WebSocket 握手拦截器直接依赖此类。
 * </p>
 */
@Component
@RequiredArgsConstructor
public class MonitorJwtUtil {

    private final JwtUtil jwtUtil;

    /**
     * 校验 Token 是否有效
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }

    /**
     * 从 Token 获取 userId（字符串形式）
     */
    public String getUserId(String token) {
        try {
            Long uid = jwtUtil.getUserIdFromToken(token);
            return uid != null ? uid.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 获取 tenantId
     * <p>约定：tenantId 存放在 JWT claim "tenantId" 字段中</p>
     */
    public String getTenantId(String token) {
        try {
            Claims claims = jwtUtil.parseToken(token);
            Object tenantId = claims.get("tenantId");
            return tenantId != null ? tenantId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
