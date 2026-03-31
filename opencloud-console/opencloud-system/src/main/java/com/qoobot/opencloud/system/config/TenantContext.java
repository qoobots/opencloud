package com.qoobot.opencloud.system.config;

/**
 * 租户上下文（ThreadLocal 存储当前请求的 tenantId）
 * 由 TenantInterceptor 在请求进入时从 JWT claims 中解析并设置
 */
public class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new InheritableThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
