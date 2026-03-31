package com.qoobot.opencloud.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求链路追踪过滤器
 * <p>
 * 每个请求进入时生成一个唯一 traceId，注入 MDC，使日志中可以按 traceId 检索整条请求链路。
 * </p>
 *
 * <b>优先级：</b>最高，确保 traceId 在所有后续过滤器 / 拦截器 / 日志之前注入。
 *
 * <b>traceId 来源（优先级从高到低）：</b>
 * <ol>
 *   <li>请求 Header {@code X-Trace-Id}（网关 / 上游服务传递时使用）</li>
 *   <li>自动生成 16 位十六进制随机串</li>
 * </ol>
 */
@Component
@Order(Integer.MIN_VALUE)
public class TraceIdFilter implements Filter {

    /** MDC Key，对应 logback-spring.xml 中 %X{traceId} */
    private static final String TRACE_ID_KEY = "traceId";

    /** 响应 Header，方便前端联调时回传 */
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 优先从 Header 读取（支持网关传递）
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }

        MDC.put(TRACE_ID_KEY, traceId);
        // 响应头回传 traceId，方便前端排查问题
        httpResponse.setHeader(TRACE_ID_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            // 请求结束后必须清理 MDC，防止线程池复用时出现 traceId 污染
            MDC.clear();
        }
    }
}
