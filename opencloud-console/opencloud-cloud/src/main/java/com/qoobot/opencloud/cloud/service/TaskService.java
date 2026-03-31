package com.qoobot.opencloud.cloud.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.cloud.domain.entity.CloudTask;
import com.qoobot.opencloud.cloud.domain.enums.TaskStatus;
import com.qoobot.opencloud.cloud.domain.enums.TaskType;
import com.qoobot.opencloud.cloud.mapper.CloudTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 异步任务服务
 */
@Slf4j
@Service
public class TaskService {

    @Autowired
    private CloudTaskMapper taskMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String TASK_CACHE_KEY = "cloud:task:%s";
    private static final long TASK_CACHE_TTL_DAYS = 7;

    /**
     * 创建任务
     */
    @Transactional
    public CloudTask createTask(String tenantId, TaskType type, 
                                 String resourceType, String clusterId,
                                 String createdBy) {
        CloudTask task = new CloudTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setTenantId(tenantId);
        task.setType(type.getCode());
        task.setStatus(TaskStatus.PENDING.getCode());
        task.setResourceType(resourceType);
        task.setClusterId(clusterId);
        task.setProgress(0);
        task.setCreatedBy(createdBy);
        task.setCreatedAt(LocalDateTime.now());

        taskMapper.insert(task);
        cacheTask(task);

        return task;
    }

    /**
     * 开始执行任务
     */
    @Transactional
    public void startTask(String taskId) {
        CloudTask task = getTaskByTaskId(taskId);
        if (task == null) {
            return;
        }

        task.setStatus(TaskStatus.RUNNING.getCode());
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        evictTaskCache(taskId);
    }

    /**
     * 更新任务进度
     */
    @Transactional
    public void updateProgress(String taskId, int progress) {
        CloudTask task = getTaskByTaskId(taskId);
        if (task == null) {
            return;
        }

        int clampedProgress = Math.min(100, Math.max(0, progress));
        task.setProgress(clampedProgress);
        taskMapper.updateById(task);
    }

    /**
     * 完成任务
     */
    @Transactional
    public void completeTask(String taskId, Object result) {
        CloudTask task = getTaskByTaskId(taskId);
        if (task == null) {
            return;
        }

        task.setStatus(TaskStatus.SUCCESS.getCode());
        task.setProgress(100);
        if (result != null) {
            task.setResultJson(result.toString());
        }
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        evictTaskCache(taskId);
    }

    /**
     * 任务失败
     */
    @Transactional
    public void failTask(String taskId, String errorMsg) {
        CloudTask task = getTaskByTaskId(taskId);
        if (task == null) {
            return;
        }

        task.setStatus(TaskStatus.FAILED.getCode());
        task.setErrorMsg(errorMsg);
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);

        evictTaskCache(taskId);
    }

    /**
     * 获取任务详情
     */
    public CloudTask getTask(String taskId) {
        return getTaskByTaskId(taskId);
    }

    /**
     * 任务列表
     */
    public Page<CloudTask> listTasks(String tenantId, String resourceType, 
                                      String status, int page, int size) {
        LambdaQueryWrapper<CloudTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CloudTask::getTenantId, tenantId)
               .orderByDesc(CloudTask::getCreatedAt);

        if (resourceType != null && !resourceType.isEmpty()) {
            wrapper.eq(CloudTask::getResourceType, resourceType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(CloudTask::getStatus, status);
        }

        return taskMapper.selectPage(new Page<>(page, size), wrapper);
    }

    private CloudTask getTaskByTaskId(String taskId) {
        // 先查缓存
        String cacheKey = String.format(TASK_CACHE_KEY, taskId);
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            // 简化处理，实际需要反序列化
            return taskMapper.selectByTaskId(taskId);
        }

        CloudTask task = taskMapper.selectByTaskId(taskId);
        if (task != null) {
            cacheTask(task);
        }
        return task;
    }

    private void cacheTask(CloudTask task) {
        String cacheKey = String.format(TASK_CACHE_KEY, task.getTaskId());
        redisTemplate.opsForValue().set(cacheKey, "1",
                TASK_CACHE_TTL_DAYS, TimeUnit.DAYS);
    }

    private void evictTaskCache(String taskId) {
        String cacheKey = String.format(TASK_CACHE_KEY, taskId);
        redisTemplate.delete(cacheKey);
    }
}
