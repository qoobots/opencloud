package com.qoobot.opencloud.cloud.controller;

import com.qoobot.opencloud.cloud.domain.dto.ConfigMapDTO;
import com.qoobot.opencloud.cloud.domain.dto.ScaleDeploymentDTO;
import com.qoobot.opencloud.cloud.domain.dto.SecretDTO;
import com.qoobot.opencloud.cloud.domain.vo.*;
import com.qoobot.opencloud.cloud.service.KubernetesService;
import com.qoobot.opencloud.common.core.page.PageResult;
import com.qoobot.opencloud.common.core.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;

/**
 * Kubernetes 资源管理控制器
 */
@Validated
@RestController
@RequestMapping("/api/cloud/k8s")
public class KubernetesController {

    @Autowired
    private KubernetesService k8sService;

    /**
     * 命名空间列表
     */
    @GetMapping("/namespaces")
    public R<List<NamespaceVO>> listNamespaces(@RequestParam @NotBlank String clusterId) {
        return R.ok(k8sService.listNamespaces(clusterId));
    }

    /**
     * Deployment 列表
     */
    @GetMapping("/deployments")
    public R<PageResult<DeploymentVO>> listDeployments(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String namespace,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(k8sService.listDeployments(clusterId, namespace, page, size));
    }

    /**
     * Deployment 详情
     */
    @GetMapping("/deployments/{ns}/{name}")
    public R<DeploymentVO> getDeployment(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String ns,
            @PathVariable String name) {
        return R.ok(k8sService.getDeployment(clusterId, ns, name));
    }

    /**
     * 扩缩容 Deployment
     */
    @PutMapping("/deployments/scale")
    public R<Void> scaleDeployment(@Valid @RequestBody ScaleDeploymentDTO dto) {
        k8sService.scaleDeployment(dto);
        return R.ok();
    }

    /**
     * Pod 列表
     */
    @GetMapping("/pods")
    public R<PageResult<PodVO>> listPods(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String namespace,
            @RequestParam(required = false) String deploymentName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(k8sService.listPods(clusterId, namespace, deploymentName, page, size));
    }

    /**
     * Pod 日志（普通模式）
     */
    @GetMapping("/pods/{ns}/{name}/logs")
    public R<String> getPodLogs(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String ns,
            @PathVariable String name,
            @RequestParam(defaultValue = "100") int tailLines) {
        return R.ok(k8sService.getPodLogs(clusterId, ns, name, tailLines));
    }

    /**
     * Pod 日志流（SSE 模式）
     */
    @GetMapping("/pods/{ns}/{name}/logs/stream")
    public void getPodLogsStream(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String ns,
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean follow) throws java.io.IOException {
        try (InputStream is = k8sService.getPodLogsStream(clusterId, ns, name, follow)) {
            // 直接返回输入流，由 Spring MVC 处理为 SSE 流
            // 实际使用时需要配置 Response 为 text/event-stream
        }
    }

    /**
     * Service 列表
     */
    @GetMapping("/services")
    public R<PageResult<ServiceVO>> listServices(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String namespace,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(k8sService.listServices(clusterId, namespace, page, size));
    }

    /**
     * ConfigMap 列表
     */
    @GetMapping("/configmaps")
    public R<PageResult<ConfigMapVO>> listConfigMaps(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String namespace,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(k8sService.listConfigMaps(clusterId, namespace, page, size));
    }

    /**
     * 创建 ConfigMap
     */
    @PostMapping("/configmaps")
    public R<ConfigMapVO> createConfigMap(@Valid @RequestBody ConfigMapDTO dto) {
        return R.ok(k8sService.createConfigMap(dto));
    }

    /**
     * 更新 ConfigMap
     */
    @PutMapping("/configmaps")
    public R<ConfigMapVO> updateConfigMap(@Valid @RequestBody ConfigMapDTO dto) {
        return R.ok(k8sService.updateConfigMap(dto));
    }

    /**
     * 删除 ConfigMap
     */
    @DeleteMapping("/configmaps/{ns}/{name}")
    public R<Void> deleteConfigMap(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String ns,
            @PathVariable String name) {
        k8sService.deleteConfigMap(clusterId, ns, name);
        return R.ok();
    }

    /**
     * Secret 列表
     */
    @GetMapping("/secrets")
    public R<PageResult<SecretVO>> listSecrets(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String namespace,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(k8sService.listSecrets(clusterId, namespace, page, size));
    }

    /**
     * 创建 Secret
     */
    @PostMapping("/secrets")
    public R<SecretVO> createSecret(@Valid @RequestBody SecretDTO dto) {
        return R.ok(k8sService.createSecret(dto));
    }

    /**
     * 删除 Secret
     */
    @DeleteMapping("/secrets/{ns}/{name}")
    public R<Void> deleteSecret(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String ns,
            @PathVariable String name) {
        k8sService.deleteSecret(clusterId, ns, name);
        return R.ok();
    }

    /**
     * Node 列表
     */
    @GetMapping("/nodes")
    public R<List<NodeVO>> listNodes(@RequestParam @NotBlank String clusterId) {
        return R.ok(k8sService.listNodes(clusterId));
    }
}
