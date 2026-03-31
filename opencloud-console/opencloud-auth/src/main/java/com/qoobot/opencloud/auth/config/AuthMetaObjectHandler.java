package com.qoobot.opencloud.auth.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * auth 模块 MyBatis-Plus 字段自动填充处理器
 *
 * <p>负责填充 auth 模块实体的 createdAt / updatedAt 字段。
 * <p>auth 模块实体不继承 BaseEntity，使用独立的时间字段命名（createdAt/updatedAt）。
 * <p>{@code @Primary} 使此 Bean 优先于 common 模块的 {@code MybatisPlusMetaHandler}。
 */
@Slf4j
@Primary
@Component
public class AuthMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        // auth 实体使用 createdAt / updatedAt
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        // common 实体兼容（createTime / updateTime / deleted）
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deleted",    Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictUpdateFill(metaObject, "updatedAt",  LocalDateTime.class, now);
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
    }
}
