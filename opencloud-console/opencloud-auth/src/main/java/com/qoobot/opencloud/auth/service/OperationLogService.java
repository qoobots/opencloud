package com.qoobot.opencloud.auth.service;

import com.qoobot.opencloud.auth.domain.entity.OperationLog;
import com.qoobot.opencloud.auth.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作审计日志服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogService {

    private final OperationLogMapper operationLogMapper;

    /**
     * 异步写入操作审计日志
     *
     * @param operatorId    操作人 userId
     * @param targetUserId  被操作 userId
     * @param operationType CHANGE_PASSWORD / FORCE_LOGOUT / UNLOCK_ACCOUNT
     */
    @Async
    public void record(String operatorId, String targetUserId, String operationType) {
        try {
            OperationLog opLog = new OperationLog();
            opLog.setOperatorId(operatorId);
            opLog.setTargetUserId(targetUserId);
            opLog.setOperationType(operationType);
            operationLogMapper.insert(opLog);
        } catch (Exception e) {
            log.warn("操作日志写入失败: operatorId={}, targetUserId={}, type={}",
                    operatorId, targetUserId, operationType, e);
        }
    }
}
