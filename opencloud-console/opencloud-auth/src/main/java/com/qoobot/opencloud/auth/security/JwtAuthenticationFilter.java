package com.qoobot.opencloud.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.auth.exception.AuthException;
import com.qoobot.opencloud.auth.service.TokenService;
import com.qoobot.opencloud.common.result.R;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 认证过滤器
 * 执行顺序：
 * 1. 提取 Bearer Token
 * 2. 校验签名 & 过期
 * 3. 黑名单检查
 * 4. tokenVersion 比对（强制下线检测）
 * 5. 构建 Authentication，写入 SecurityContext
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider    jwtTokenProvider;
    private final TokenService        tokenService;
    private final ObjectMapper        objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String token = extractBearerToken(request);
        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 2. 校验签名 & 过期
            Claims claims = jwtTokenProvider.validateAndParseClaims(token);

            // 3. 黑名单检查
            String jti = jwtTokenProvider.parseJti(claims);
            if (tokenService.isRevoked(jti)) {
                sendError(response, AuthException.tokenRevoked());
                return;
            }

            // 4. tokenVersion 比对
            String userId        = jwtTokenProvider.parseUserId(claims);
            int    tokenVersion  = jwtTokenProvider.parseTokenVersion(claims);
            int    latestVersion = tokenService.getTokenVersion(userId);
            if (tokenVersion < latestVersion) {
                sendError(response, AuthException.tokenVersionMismatch());
                return;
            }

            // 5. 构建 Authentication
            List<String> roles = jwtTokenProvider.parseRoles(claims);
            List<SimpleGrantedAuthority> authorities = (roles == null ? List.<String>of() : roles)
                    .stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 将完整 claims 附加到 details 供 Service 层取用
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 将 claims 存入 request attribute，方便 Controller/Service 取 jti / userId
            request.setAttribute("jwtClaims", claims);

        } catch (ExpiredJwtException e) {
            sendError(response, AuthException.tokenExpired());
            return;
        } catch (JwtException e) {
            sendError(response, AuthException.tokenInvalid());
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private void sendError(HttpServletResponse response, AuthException ex) throws IOException {
        response.setStatus(ex.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        R<Void> body = R.fail(ex.getErrorCode(), ex.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
