package com.qoobot.opencloud.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Security 认证/授权失败处理器 —— 返回 JSON 而非重定向
 */
@Component
@RequiredArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** 未认证（401） */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, R.fail(ResultCode.UNAUTHORIZED));
    }

    /** 无权限（403） */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        writeJson(response, HttpServletResponse.SC_FORBIDDEN, R.fail(ResultCode.FORBIDDEN));
    }

    private void writeJson(HttpServletResponse response, int status, R<?> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
