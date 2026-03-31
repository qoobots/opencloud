package com.qoobot.opencloud.cloud.service;

import com.qoobot.opencloud.cloud.domain.dto.ConfigMapDTO;
import com.qoobot.opencloud.cloud.domain.dto.ScaleDeploymentDTO;
import com.qoobot.opencloud.cloud.domain.dto.SecretDTO;
import com.qoobot.opencloud.cloud.domain.vo.*;
import com.qoobot.opencloud.common.core.page.PageResult;

import java.io.InputStream;
import java.util.List;

/**
 * Kubernetes 服务接口
 */
public interface KubernetesService {

    /**
     * 获取命名空间列表
     */
    List<NamespaceVO> listNamespaces(String clusterId);

    /**
     * 获取 Deployment 列表
     */
    PageResult<DeploymentVO> listDeployments(String clusterId, String namespace, int page, int size);

    /**
     * 获取 Deployment 详情
     */
    DeploymentVO getDeployment(String clusterId, String namespace, String name);

    /**
     * 扩缩容 Deployment
     */
    void scaleDeployment(ScaleDeploymentDTO dto);

    /**
     * 获取 Pod 列表
     */
    PageResult<PodVO> listPods(String clusterId, String namespace, String deploymentName, int page, int size);

    /**
     * 获取 Pod 日志（普通模式）
     */
    String getPodLogs(String clusterId, String namespace, String podName, int tailLines);

    /**
     * 获取 Pod 日志流（SSE 模式）
     */
    InputStream getPodLogsStream(String clusterId, String namespace, String podName, boolean follow);

    /**
     * 获取 Service 列表
     */
    PageResult<ServiceVO> listServices(String clusterId, String namespace, int page, int size);

    /**
     * 获取 ConfigMap 列表
     */
    PageResult<ConfigMapVO> listConfigMaps(String clusterId, String namespace, int page, int size);

    /**
     * 创建 ConfigMap
     */
    ConfigMapVO createConfigMap(ConfigMapDTO dto);

    /**
     * 更新 ConfigMap
     */
    ConfigMapVO updateConfigMap(ConfigMapDTO dto);

    /**
     * 删除 ConfigMap
     */
    void deleteConfigMap(String clusterId, String namespace, String name);

    /**
     * 获取 Secret 列表
     */
    PageResult<SecretVO> listSecrets(String clusterId, String namespace, int page, int size);

    /**
     * 创建 Secret
     */
    SecretVO createSecret(SecretDTO dto);

    /**
     * 删除 Secret
     */
    void deleteSecret(String clusterId, String namespace, String name);

    /**
     * 获取 Node 列表
     */
    List<NodeVO> listNodes(String clusterId);
}
