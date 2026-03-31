package com.qoobot.opencloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OpenCloud 控制台启动类
 */
@SpringBootApplication(scanBasePackages = "com.qoobot.opencloud")
@MapperScan("com.qoobot.opencloud.**.mapper")
@EnableAsync
@EnableScheduling
public class OpenCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenCloudApplication.class, args);
        System.out.println("""
                
                ╔══════════════════════════════════════════════════╗
                ║        OpenCloud Console  启动成功 🚀             ║
                ║  API 文档: http://localhost:8080/doc.html         ║
                ╚══════════════════════════════════════════════════╝
                """);
    }
}
