package com.qoobot.opencloud.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 定时任务调度线程池配置
 * <p>
 * 覆盖 Spring 默认的单线程调度器，使用多线程调度池防止任务互相阻塞：
 * <ul>
 *   <li>线程池大小 10 —— 支持最多 10 个任务并行执行</li>
 *   <li>优雅停机：等待当前执行中的任务完成（最长 30 秒）</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableScheduling
public class SchedulerConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("opencloud-sched-");
        // 优雅停机
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        // 调度任务异常不中断整个调度器
        scheduler.setErrorHandler(t ->
                org.slf4j.LoggerFactory.getLogger(SchedulerConfig.class)
                        .error("[Scheduler] 定时任务执行异常", t));
        scheduler.initialize();
        taskRegistrar.setTaskScheduler(scheduler);
    }
}
