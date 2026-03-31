package com.qoobot.opencloud.system.log.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 使用方式：在 Controller 方法上加 @SysLog(module="用户管理", action="新增用户")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SysLog {

    /** 功能模块名，如"用户管理" */
    String module() default "";

    /** 操作描述，如"新增用户" */
    String action() default "";
}
