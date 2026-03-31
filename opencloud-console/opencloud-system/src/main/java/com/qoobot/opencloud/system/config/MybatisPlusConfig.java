package com.qoobot.opencloud.system.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * MyBatis-Plus 配置：租户隔离 + 分页插件
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineHandler tenantLineHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 租户隔离
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));
        // 2. 分页
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }

    @Bean
    public TenantLineHandler tenantLineHandler() {
        return new SystemTenantHandler();
    }

    /**
     * 系统租户隔离处理器
     */
    static class SystemTenantHandler implements TenantLineHandler {

        /** 从 SecurityContext 获取当前用户的 tenantId（存储在 request attribute 中） */
        @Override
        public Expression getTenantId() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                // tenantId 由 JwtTokenProvider 解析 JWT claims 后写入
                // 这里返回 UNKNOWN 作为兜底，实际 tenantId 由 Service 层传入或从 claims 取
                Object principal = auth.getPrincipal();
                if (principal instanceof String) {
                    // 从 ThreadLocal 或 RequestContextHolder 取 tenantId
                    String tenantId = TenantContext.getTenantId();
                    if (tenantId != null) {
                        return new StringValue(tenantId);
                    }
                }
            }
            return new StringValue("__UNKNOWN__");
        }

        @Override
        public String getTenantIdColumn() {
            return "tenant_id";
        }

        /**
         * 不做租户过滤的表：
         * - sys_menu / sys_role_menu：菜单为全局共享数据
         * - sys_dict_type / sys_dict_data：字典为全局共享数据
         * - sys_tenant：租户表本身（超级管理员全局查看）
         * - auth_login_log：auth 模块的表（自身逻辑控制）
         * - sys_operation_log：日志按租户分区，但在服务层显式传入
         */
        @Override
        public boolean ignoreTable(String tableName) {
            return Set.of(
                    "sys_menu", "sys_role_menu",
                    "sys_dict_type", "sys_dict_data",
                    "sys_tenant",
                    "auth_login_log",
                    "sys_operation_log"
            ).contains(tableName);
        }
    }

    /**
     * 系统模块 MetaObjectHandler（自动填充 createdAt/updatedAt）
     */
    @Primary
    @Component("systemMetaObjectHandler")
    static class SystemMetaObjectHandler implements MetaObjectHandler {

        @Override
        public void insertFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
            this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            this.strictInsertFill(metaObject, "deleted",   Integer.class, 0);
            // 兼容 common 模块 BaseEntity 字段名
            this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
            this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        }

        @Override
        public void updateFill(MetaObject metaObject) {
            LocalDateTime now = LocalDateTime.now();
            this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, now);
            this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        }
    }
}
