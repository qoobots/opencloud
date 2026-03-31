package com.qoobot.opencloud.auth;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * opencloud-auth 启动类
 *
 * <p>认证授权模块，提供：
 * <ul>
 *   <li>JWT 双 Token 认证（Access + Refresh）</li>
 *   <li>RBAC 权限模型</li>
 *   <li>登录限频、账号锁定、强制下线</li>
 *   <li>图形验证码</li>
 * </ul>
 */
@SpringBootApplication(scanBasePackages = "com.qoobot.opencloud")
@MapperScan("com.qoobot.opencloud.auth.mapper")
@EnableAsync
public class OpencloudAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpencloudAuthApplication.class, args);
    }
}
