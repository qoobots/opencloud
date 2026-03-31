package com.qoobot.opencloud.system.log.service;

import com.qoobot.opencloud.system.log.domain.entity.SysOperationLog;

/**
 * 操作日志 Service
 */
public interface OperationLogService {

    /** 异步写入操作日志 */
    void saveAsync(SysOperationLog log);
}
