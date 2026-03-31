package com.qoobot.opencloud.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 / Knife4j 文档配置
 * <p>
 * 按功能模块分组：认证授权 / 系统管理 / 云平台管理 / 监控告警
 * </p>
 * <p>
 * 访问地址：http://localhost:8080/doc.html
 * </p>
 */
@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "BearerAuth";

    // ─── 全局 API 信息 ────────────────────────────────────────────

    @Bean
    public OpenAPI openApiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("OpenCloud 控制台 API")
                        .description("""
                                OpenCloud 云计算管理平台统一接口文档。
                                
                                **认证方式：** Bearer Token（在 Header 中携带 `Authorization: Bearer <accessToken>`）
                                
                                **模块说明：**
                                - 认证授权（/auth）：登录、登出、Token 刷新、验证码
                                - 系统管理（/system）：用户、角色、菜单、日志管理
                                - 云平台管理（/cloud）：集群、云主机、网络、存储、K8s
                                - 监控告警（/monitor）：指标查询、告警规则、通知渠道
                                """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("OpenCloud 开发团队")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请先登录获取 accessToken，填入此处（无需 Bearer 前缀）")));
    }

    // ─── 模块分组 ────────────────────────────────────────────────

    @Bean
    public GroupedOpenApi authApi() {
        return GroupedOpenApi.builder()
                .group("01-认证授权")
                .displayName("01 - 认证授权（Auth）")
                .pathsToMatch("/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi systemApi() {
        return GroupedOpenApi.builder()
                .group("02-系统管理")
                .displayName("02 - 系统管理（System）")
                .pathsToMatch("/system/**")
                .build();
    }

    @Bean
    public GroupedOpenApi cloudApi() {
        return GroupedOpenApi.builder()
                .group("03-云平台管理")
                .displayName("03 - 云平台管理（Cloud）")
                .pathsToMatch("/cloud/**")
                .build();
    }

    @Bean
    public GroupedOpenApi monitorApi() {
        return GroupedOpenApi.builder()
                .group("04-监控告警")
                .displayName("04 - 监控告警（Monitor）")
                .pathsToMatch("/monitor/**")
                .build();
    }
}
