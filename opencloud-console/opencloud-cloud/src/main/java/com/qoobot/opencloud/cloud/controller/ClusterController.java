package com.qoobot.opencloud.cloud.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.cloud.domain.entity.ClusterConfig;
import com.qoobot.opencloud.cloud.service.ClusterService;
import com.qoobot.opencloud.common.util.SecurityUtils;
import com.qoobot.opencloud.common.web.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 集群配置管理控制器
 */
@RestController
@RequestMapping("/api/cloud/clusters")
public class ClusterController {

    @Autowired
    private ClusterService clusterService;

    /**
     * 获取集群列表
     */
    @GetMapping
    @PreAuthorize("hasAuthority('cloud:cluster:list')")
    public R<Page<ClusterConfig>> listClusters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type) {
        
        String tenantId = SecurityUtils.getCurrentTenantId();
        Page<ClusterConfig> result = clusterService.listClusters(tenantId, type, page, size);
        return R.ok(result);
    }

    /**
     * 获取集群详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('cloud:cluster:list')")
    public R<ClusterConfig> getCluster(@PathVariable String id) {
        String tenantId = SecurityUtils.getCurrentTenantId();
        ClusterConfig config = clusterService.getCluster(id, tenantId);
        // 脱敏处理
        config.setConfigJson("***");
        return R.ok(config);
    }

    /**
     * 新增集群配置
     */
    @PostMapping
    @PreAuthorize("hasAuthority('cloud:cluster:add')")
    public R<String> createCluster(@RequestBody ClusterConfig config) {
        String tenantId = SecurityUtils.getCurrentTenantId();
        String userId = SecurityUtils.getCurrentUserId();
        
        config.setTenantId(tenantId);
        String id = clusterService.createCluster(config, userId);
        return R.ok(id);
    }

    /**
     * 编辑集群配置
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('cloud:cluster:edit')")
    public R<Void> updateCluster(@PathVariable String id, @RequestBody ClusterConfig config) {
        String tenantId = SecurityUtils.getCurrentTenantId();
        String userId = SecurityUtils.getCurrentUserId();
        
        clusterService.updateCluster(id, config, tenantId, userId);
        return R.ok();
    }

    /**
     * 删除集群配置
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('cloud:cluster:delete')")
    public R<Void> deleteCluster(@PathVariable String id) {
        String tenantId = SecurityUtils.getCurrentTenantId();
        clusterService.deleteCluster(id, tenantId);
        return R.ok();
    }

    /**
     * 连接测试
     */
    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('cloud:cluster:test')")
    public R<Map<String, Object>> testConnection(@PathVariable String id) {
        String tenantId = SecurityUtils.getCurrentTenantId();
        Map<String, Object> result = clusterService.testConnection(id, tenantId);
        return R.ok(result);
    }
}
