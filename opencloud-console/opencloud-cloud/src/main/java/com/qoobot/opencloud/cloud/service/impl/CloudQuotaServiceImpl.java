package com.qoobot.opencloud.cloud.service.impl;

import com.qoobot.opencloud.cloud.client.CloudClientFactory;
import com.qoobot.opencloud.cloud.domain.dto.InstanceCreateDTO;
import com.qoobot.opencloud.cloud.domain.entity.CloudQuota;
import com.qoobot.opencloud.cloud.exception.CloudException;
import com.qoobot.opencloud.cloud.mapper.CloudQuotaMapper;
import com.qoobot.opencloud.cloud.service.CloudQuotaService;
import com.qoobot.opencloud.common.core.utils.TenantContext;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Flavor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 云配额服务实现
 */
@Slf4j
@Service
public class CloudQuotaServiceImpl implements CloudQuotaService {

    @Autowired
    private CloudQuotaMapper quotaMapper;

    @Autowired
    private CloudClientFactory clientFactory;

    @Override
    public void checkAndReserveQuota(String clusterId, InstanceCreateDTO dto) {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new CloudException("QUOTA_0001", "无法获取租户 ID");
        }

        CloudQuota quota = quotaMapper.selectByTenantId(tenantId);
        if (quota == null) {
            throw new CloudException("QUOTA_0002", "租户未配置配额，请联系管理员");
        }

        // 获取规格信息
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        Flavor flavor = os.compute().flavors().get(dto.getFlavorId());
        if (flavor == null) {
            throw new CloudException("OS_0002", "规格不存在");
        }

        // 解析配额配置
        Map<String, Object> openstackQuota = parseQuota(quota.getOpenstackQuota());

        int vcpuLimit = getIntValue(openstackQuota, "vcpuLimit", 0);
        int memoryLimit = getIntValue(openstackQuota, "memoryLimit", 0);
        int instanceLimit = getIntValue(openstackQuota, "instanceLimit", 0);

        // 计算当前使用量
        int vcpuUsed = getCurrentVcpuUsage(os, tenantId);
        int memoryUsed = getCurrentMemoryUsage(os, tenantId);
        int instanceUsed = getCurrentInstanceCount(os, tenantId);

        // 计算新增需求
        int count = dto.getCount() != null ? dto.getCount() : 1;
        int newVcpus = flavor.getVcpus() * count;
        int newMemory = flavor.getRam() * count;

        // 校验配额
        if (vcpuLimit >= 0 && vcpuUsed + newVcpus > vcpuLimit) {
            throw new CloudException("QUOTA_0003",
                    String.format("vCPU 配额不足: 已使用 %d, 需要 %d, 上限 %d", vcpuUsed, newVcpus, vcpuLimit));
        }

        if (memoryLimit >= 0 && memoryUsed + newMemory > memoryLimit) {
            throw new CloudException("QUOTA_0004",
                    String.format("内存配额不足: 已使用 %d MB, 需要 %d MB, 上限 %d MB", memoryUsed, newMemory, memoryLimit));
        }

        if (instanceLimit >= 0 && instanceUsed + count > instanceLimit) {
            throw new CloudException("QUOTA_0005",
                    String.format("云主机数量配额不足: 已创建 %d, 需要创建 %d, 上限 %d", instanceUsed, count, instanceLimit));
        }
    }

    @Override
    public void releaseQuota(String clusterId, String flavorId) {
        // 配额释放逻辑 - 实际配额使用量是通过实时查询统计的
        // 这里可以记录释放日志或触发配额重新计算
    }

    @Override
    public Map<String, Object> getQuota(String tenantId) {
        CloudQuota quota = quotaMapper.selectByTenantId(tenantId);
        if (quota == null) {
            return Map.of();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", quota.getId());
        result.put("tenantId", quota.getTenantId());
        result.put("openstackQuota", parseQuota(quota.getOpenstackQuota()));
        result.put("createdBy", quota.getCreatedBy());
        result.put("createdAt", quota.getCreatedAt());
        return result;
    }

    @Override
    public void setQuota(String tenantId, Map<String, Object> quota) {
        CloudQuota existing = quotaMapper.selectByTenantId(tenantId);

        // 解析配额配置
        String openstackQuotaJson = parseQuotaJson(quota);

        if (existing == null) {
            // 创建新配额
            CloudQuota newQuota = new CloudQuota();
            newQuota.setTenantId(tenantId);
            newQuota.setOpenstackQuota(openstackQuotaJson);
            newQuota.setCreatedBy(TenantContext.getTenantId());
            newQuota.setCreatedAt(java.time.LocalDateTime.now());
            newQuota.setUpdatedAt(java.time.LocalDateTime.now());
            quotaMapper.insert(newQuota);
        } else {
            // 更新配额
            existing.setOpenstackQuota(openstackQuotaJson);
            existing.setUpdatedAt(java.time.LocalDateTime.now());
            quotaMapper.updateById(existing);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseQuota(String json) {
        if (json == null || json.isEmpty()) {
            return Map.of();
        }
        // 简化实现，实际使用 JSON 库解析
        return new HashMap<>();
    }

    private String parseQuotaJson(Map<String, Object> quota) {
        // 简化实现，实际使用 JSON 库序列化
        return new java.util.HashMap<>(quota).toString();
    }

    private int getIntValue(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }

    private int getCurrentVcpuUsage(OSClient.OSClientV3 os, String tenantId) {
        return os.compute().servers().list(true).stream()
                .filter(s -> tenantId.equals(s.getTenantId()))
                .mapToInt(s -> {
                    Flavor f = s.getFlavor();
                    return f != null ? f.getVcpus() : 0;
                })
                .sum();
    }

    private int getCurrentMemoryUsage(OSClient.OSClientV3 os, String tenantId) {
        return os.compute().servers().list(true).stream()
                .filter(s -> tenantId.equals(s.getTenantId()))
                .mapToInt(s -> {
                    Flavor f = s.getFlavor();
                    return f != null ? f.getRam() : 0;
                })
                .sum();
    }

    private int getCurrentInstanceCount(OSClient.OSClientV3 os, String tenantId) {
        return (int) os.compute().servers().list(true).stream()
                .filter(s -> tenantId.equals(s.getTenantId()))
                .count();
    }
}
