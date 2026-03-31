package com.qoobot.opencloud.cloud.service;

import com.qoobot.opencloud.cloud.domain.dto.InstanceCreateDTO;

import java.util.Map;

/**
 * 云配额服务接口
 */
public interface CloudQuotaService {

    /**
     * 检查并预留配额
     */
    void checkAndReserveQuota(String clusterId, InstanceCreateDTO dto);

    /**
     * 释放配额
     */
    void releaseQuota(String clusterId, String flavorId);

    /**
     * 获取租户配额
     */
    Map<String, Object> getQuota(String tenantId);

    /**
     * 设置租户配额
     */
    void setQuota(String tenantId, Map<String, Object> quota);
}
