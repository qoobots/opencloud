package com.qoobot.opencloud.system.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租户上下文过滤器
 * 从 JWT Claims（由 JwtAuthenticationFilter 写入 request attribute）中提取 tenantId，
 * 设置到 TenantContext ThreadLocal，供 MyBatis-Plus 租户隔离使用。
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Object claims = request.getAttribute("jwtClaims");
            if (claims instanceof Claims jwtClaims) {
                String tenantId = jwtClaims.get("tenantId", String.class);
                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                    request.setAttribute("tenantId", tenantId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
