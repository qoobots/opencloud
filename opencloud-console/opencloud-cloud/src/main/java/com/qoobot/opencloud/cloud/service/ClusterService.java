package com.qoobot.opencloud.cloud.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.cloud.client.CloudClientFactory;
import com.qoobot.opencloud.cloud.config.EncryptUtil;
import com.qoobot.opencloud.cloud.domain.entity.ClusterConfig;
import com.qoobot.opencloud.cloud.domain.enums.ClusterStatus;
import com.qoobot.opencloud.cloud.domain.enums.ClusterType;
import com.qoobot.opencloud.cloud.exception.CloudException;
import com.qoobot.opencloud.cloud.mapper.ClusterConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 集群配置服务
 */
@Slf4j
@Service
public class ClusterService {

    @Autowired
    private ClusterConfigMapper clusterConfigMapper;

    @Autowired
    private EncryptUtil encryptUtil;

    @Autowired
    private CloudClientFactory clientFactory;

    /**
     * 获取集群列表
     */
    public Page<ClusterConfig> listClusters(String tenantId, String type, int page, int size) {
        LambdaQueryWrapper<ClusterConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClusterConfig::getTenantId, tenantId)
               .eq(ClusterConfig::getDeleted, 0)
               .orderByDesc(ClusterConfig::getCreatedAt);

        if (type != null && !type.isEmpty()) {
            wrapper.eq(ClusterConfig::getType, type);
        }

        return clusterConfigMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 获取集群详情
     */
    public ClusterConfig getCluster(String id, String tenantId) {
        LambdaQueryWrapper<ClusterConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClusterConfig::getId, id)
               .eq(ClusterConfig::getTenantId, tenantId)
               .eq(ClusterConfig::getDeleted, 0);

        ClusterConfig config = clusterConfigMapper.selectOne(wrapper);
        if (config == null) {
            throw new CloudException("CLOUD_0001", "集群配置不存在", HttpStatus.NOT_FOUND);
        }
        return config;
    }

    /**
     * 新增集群配置
     */
    @Transactional
    public String createCluster(ClusterConfig config, String userId) {
        // 检查名称唯一性
        LambdaQueryWrapper<ClusterConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClusterConfig::getTenantId, config.getTenantId())
               .eq(ClusterConfig::getName, config.getName())
               .eq(ClusterConfig::getDeleted, 0);

        if (clusterConfigMapper.selectCount(wrapper) > 0) {
            throw new CloudException("CLOUD_0105", "集群名称已存在");
        }

        // 校验类型
        if (ClusterType.fromCode(config.getType()) == null) {
            throw new CloudException("CLOUD_0103", "无效的集群类型");
        }

        // 加密敏感配置
        config.setConfigJson(encryptUtil.encrypt(config.getConfigJson()));
        config.setStatus(ClusterStatus.PENDING.getCode());
        config.setCreatedBy(userId);
        config.setUpdatedBy(userId);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setDeleted(0);

        clusterConfigMapper.insert(config);
        return String.valueOf(config.getId());
    }

    /**
     * 编辑集群配置
     */
    @Transactional
    public void updateCluster(String id, ClusterConfig updateConfig, String tenantId, String userId) {
        ClusterConfig existing = getCluster(id, tenantId);

        // 检查名称唯一性（排除自身）
        if (updateConfig.getName() != null && !updateConfig.getName().equals(existing.getName())) {
            LambdaQueryWrapper<ClusterConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ClusterConfig::getTenantId, tenantId)
                   .eq(ClusterConfig::getName, updateConfig.getName())
                   .eq(ClusterConfig::getDeleted, 0)
                   .ne(ClusterConfig::getId, id);

            if (clusterConfigMapper.selectCount(wrapper) > 0) {
                throw new CloudException("CLOUD_0105", "集群名称已存在");
            }
            existing.setName(updateConfig.getName());
        }

        // 更新其他字段
        if (updateConfig.getEndpoint() != null) {
            existing.setEndpoint(updateConfig.getEndpoint());
        }
        if (updateConfig.getConfigJson() != null && !updateConfig.getConfigJson().isEmpty()) {
            // 重新加密配置
            existing.setConfigJson(encryptUtil.encrypt(updateConfig.getConfigJson()));
        }

        // 配置变更后重置状态为 PENDING
        existing.setStatus(ClusterStatus.PENDING.getCode());
        existing.setUpdatedBy(userId);
        existing.setUpdatedAt(LocalDateTime.now());

        clusterConfigMapper.updateById(existing);

        // 清除客户端缓存
        clientFactory.invalidateClient(id);
    }

    /**
     * 删除集群配置
     */
    @Transactional
    public void deleteCluster(String id, String tenantId) {
        ClusterConfig config = getCluster(id, tenantId);
        config.setDeleted(1);
        config.setUpdatedAt(LocalDateTime.now());
        clusterConfigMapper.updateById(config);

        // 清除客户端缓存
        clientFactory.invalidateClient(id);
    }

    /**
     * 连接测试
     */
    public Map<String, Object> testConnection(String id, String tenantId) {
        ClusterConfig config = getCluster(id, tenantId);

        long startTime = System.currentTimeMillis();
        boolean success = false;
        String message;

        try {
            // 解密配置
            String decryptedJson = encryptUtil.decrypt(config.getConfigJson());
            config.setConfigJson(decryptedJson);

            switch (ClusterType.fromCode(config.getType())) {
                case OPENSTACK:
                    success = testOpenStackConnection(config);
                    break;
                case CEPH:
                    success = testCephConnection(config);
                    break;
                case KUBERNETES:
                    success = testKubernetesConnection(config);
                    break;
            }

            message = success ? "连接成功" : "连接失败";
        } catch (Exception e) {
            message = "连接失败: " + e.getMessage();
            log.error("集群 {} 连接测试失败", id, e);
        }

        long latencyMs = System.currentTimeMillis() - startTime;

        // 更新集群状态
        config.setStatus(success ? ClusterStatus.ACTIVE.getCode() : ClusterStatus.ERROR.getCode());
        config.setLastCheckTime(LocalDateTime.now());
        config.setErrorMsg(success ? null : message);
        config.setUpdatedAt(LocalDateTime.now());
        clusterConfigMapper.updateById(config);

        return Map.of(
            "success", success,
            "message", message,
            "latencyMs", latencyMs
        );
    }

    private boolean testOpenStackConnection(ClusterConfig config) {
        try {
            var client = clientFactory.getOpenStackClient(String.valueOf(config.getId()));
            // 尝试获取项目列表验证连接
            client.identity().projects().list();
            return true;
        } catch (Exception e) {
            log.error("OpenStack 连接测试失败", e);
            return false;
        }
    }

    private boolean testCephConnection(ClusterConfig config) {
        try {
            // 使用 WebClient 测试 Ceph MGR API
            WebClient client = WebClient.builder()
                    .baseUrl(config.getEndpoint())
                    .build();

            // 简单的健康检查端点
            var response = client.get()
                    .uri("/api/health/minimal")
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return response != null && response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Ceph 连接测试失败", e);
            return false;
        }
    }

    private boolean testKubernetesConnection(ClusterConfig config) {
        try {
            var client = clientFactory.getKubernetesClient(String.valueOf(config.getId()));
            // 尝试获取版本信息验证连接
            client.getKubernetesVersion();
            return true;
        } catch (Exception e) {
            log.error("Kubernetes 连接测试失败", e);
            return false;
        }
    }

    /**
     * 获取所有 ACTIVE 状态的集群
     */
    public List<ClusterConfig> getActiveClusters() {
        return clusterConfigMapper.selectActiveClusters();
    }
}
