package com.qoobot.opencloud.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 * <ul>
 *   <li>分页插件（PostgreSQL 方言）</li>
 *   <li>乐观锁插件（@Version 注解）</li>
 * </ul>
 *
 * <p>逻辑删除、自动填充等全局配置在 application.yml 的 mybatis-plus 节点中配置。</p>
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件，指定 PostgreSQL 方言（自动拼接 LIMIT x OFFSET y）
        PaginationInnerInterceptor paginationInterceptor =
                new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        // 单次查询最大限制 10000 条，防止恶意大页请求
        paginationInterceptor.setMaxLimit(10000L);
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 乐观锁插件（更新时自动校验并递增 @Version 字段）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }
}
