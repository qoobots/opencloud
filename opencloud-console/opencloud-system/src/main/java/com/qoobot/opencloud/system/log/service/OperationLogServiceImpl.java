package com.qoobot.opencloud.system.log.service;

import com.qoobot.opencloud.system.log.domain.entity.SysOperationLog;
import com.qoobot.opencloud.system.log.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final SysOperationLogMapper mapper;

    @Override
    @Async
    public void saveAsync(SysOperationLog opLog) {
        try {
            mapper.insert(opLog);
        } catch (Exception e) {
            log.error("操作日志写入失败: userId={}, error={}", opLog.getUserId(), e.getMessage());
        }
    }
}
