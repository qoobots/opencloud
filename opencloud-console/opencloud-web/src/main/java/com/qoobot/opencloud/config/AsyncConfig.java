package com.qoobot.opencloud.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 全局异步线程池配置
 * <p>
 * 覆盖 Spring Boot 默认的单线程异步执行器，提供高并发支持：
 * <ul>
 *   <li>核心线程数 10 —— 覆盖正常并发</li>
 *   <li>最大线程数 50 —— 覆盖告警高峰突发</li>
 *   <li>队列容量 100 —— 防止突发堆积</li>
 *   <li>拒绝策略 CallerRunsPolicy —— 降级但不丢任务</li>
 *   <li>优雅停机：等待队列任务完成（最长 30 秒）</li>
 * </ul>
 * </p>
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("opencloud-async-");
        // 队列满时在调用方线程执行，不丢任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅停机：等待已提交任务执行完毕
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("[Async] 异步任务执行异常 method={} params={}", method.getName(), params, ex);
    }
}
