package com.qoobot.opencloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 全局配置
 * <ul>
 *   <li>CORS 跨域策略</li>
 *   <li>静态资源映射</li>
 * </ul>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 允许跨域的前端域名，多个以逗号分隔。
     * 生产环境通过 ALLOWED_ORIGINS 环境变量注入具体域名，禁止使用 * 通配符。
     */
    @Value("${opencloud.web.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 将多值字符串拆分为数组
        String[] origins = allowedOrigins.split(",");

        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders(
                        "Content-Type",
                        "Authorization",
                        "X-Requested-With",
                        "X-User-Id",
                        "X-Old-Jti",
                        "X-Trace-Id"
                )
                .exposedHeaders("Authorization", "X-Trace-Id")
                // JWT 无状态，不依赖 Cookie，设为 false 避免与 allowedOriginPatterns("*") 冲突
                .allowCredentials(false)
                // 预检请求缓存 1 小时，减少 OPTIONS 请求
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Knife4j / Swagger UI 静态资源
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
