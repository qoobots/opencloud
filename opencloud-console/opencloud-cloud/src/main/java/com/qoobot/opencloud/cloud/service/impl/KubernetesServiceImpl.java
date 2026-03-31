package com.qoobot.opencloud.cloud.service.impl;

import com.qoobot.opencloud.cloud.client.CloudClientFactory;
import com.qoobot.opencloud.cloud.domain.dto.ConfigMapDTO;
import com.qoobot.opencloud.cloud.domain.dto.ScaleDeploymentDTO;
import com.qoobot.opencloud.cloud.domain.dto.SecretDTO;
import com.qoobot.opencloud.cloud.domain.vo.*;
import com.qoobot.opencloud.cloud.exception.CloudException;
import com.qoobot.opencloud.cloud.service.KubernetesService;
import com.qoobot.opencloud.common.core.page.PageResult;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Kubernetes 服务实现
 */
@Slf4j
@Service
public class KubernetesServiceImpl implements KubernetesService {

    @Autowired
    private CloudClientFactory clientFactory;

    @Override
    public List<NamespaceVO> listNamespaces(String clusterId) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        NamespaceList namespaceList = client.namespaces().list();

        return namespaceList.getItems().stream()
                .map(this::convertToNamespaceVO)
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<DeploymentVO> listDeployments(String clusterId, String namespace, int page, int size) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        DeploymentList deploymentList;
        if (namespace != null && !namespace.isEmpty()) {
            deploymentList = client.apps().deployments().inNamespace(namespace).list();
        } else {
            deploymentList = client.apps().deployments().inAnyNamespace().list();
        }

        List<Deployment> deployments = deploymentList.getItems();

        // 分页
        int total = deployments.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Deployment> pageList = start < total ? deployments.subList(start, end) : List.of();

        List<DeploymentVO> voList = pageList.stream()
                .map(this::convertToDeploymentVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public DeploymentVO getDeployment(String clusterId, String namespace, String name) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        Deployment deployment = client.apps().deployments()
                .inNamespace(namespace)
                .withName(name)
                .get();

        if (deployment == null) {
            throw new CloudException("K8S_0001", "Deployment 不存在", HttpStatus.NOT_FOUND);
        }

        return convertToDeploymentVO(deployment);
    }

    @Override
    public void scaleDeployment(ScaleDeploymentDTO dto) {
        KubernetesClient client = clientFactory.getKubernetesClient(dto.getClusterId());

        // 获取当前 Deployment
        Deployment deployment = client.apps().deployments()
                .inNamespace(dto.getNamespace())
                .withName(dto.getName())
                .get();

        if (deployment == null) {
            throw new CloudException("K8S_0001", "Deployment 不存在", HttpStatus.NOT_FOUND);
        }

        // 更新副本数
        Deployment updatedDeployment = client.apps().deployments()
                .inNamespace(dto.getNamespace())
                .withName(dto.getName())
                .scale(dto.getReplicas(), true);

        log.info("Scaled deployment {}/{} to {} replicas", dto.getNamespace(), dto.getName(), dto.getReplicas());
    }

    @Override
    public PageResult<PodVO> listPods(String clusterId, String namespace, String deploymentName, int page, int size) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        PodList podList;
        if (namespace != null && !namespace.isEmpty()) {
            if (deploymentName != null && !deploymentName.isEmpty()) {
                // 通过 deployment label 查询
                podList = client.pods().inNamespace(namespace)
                        .withLabel("app", deploymentName)
                        .list();
            } else {
                podList = client.pods().inNamespace(namespace).list();
            }
        } else {
            podList = client.pods().inAnyNamespace().list();
        }

        List<Pod> pods = podList.getItems();

        // 分页
        int total = pods.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Pod> pageList = start < total ? pods.subList(start, end) : List.of();

        List<PodVO> voList = pageList.stream()
                .map(this::convertToPodVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public String getPodLogs(String clusterId, String namespace, String podName, int tailLines) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        try {
            return client.pods().inNamespace(namespace)
                    .withName(podName)
                    .getLog(false, true, tailLines);
        } catch (Exception e) {
            log.error("Failed to get logs for pod {}/{}", namespace, podName, e);
            throw new CloudException("K8S_0002", "获取 Pod 日志失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream getPodLogsStream(String clusterId, String namespace, String podName, boolean follow) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        try {
            // fabric8 kubernetes-client 的 logs 方法返回 InputStream
            // follow=true 时会保持连接并持续输出日志
            return client.pods().inNamespace(namespace)
                    .withName(podName)
                    .getLogInputStream(follow);
        } catch (Exception e) {
            log.error("Failed to get logs stream for pod {}/{}", namespace, podName, e);
            throw new CloudException("K8S_0003", "获取 Pod 日志流失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<ServiceVO> listServices(String clusterId, String namespace, int page, int size) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        ServiceList serviceList;
        if (namespace != null && !namespace.isEmpty()) {
            serviceList = client.services().inNamespace(namespace).list();
        } else {
            serviceList = client.services().inAnyNamespace().list();
        }

        List<Service> services = serviceList.getItems();

        // 分页
        int total = services.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Service> pageList = start < total ? services.subList(start, end) : List.of();

        List<ServiceVO> voList = pageList.stream()
                .map(this::convertToServiceVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public PageResult<ConfigMapVO> listConfigMaps(String clusterId, String namespace, int page, int size) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        ConfigMapList configMapList;
        if (namespace != null && !namespace.isEmpty()) {
            configMapList = client.configMaps().inNamespace(namespace).list();
        } else {
            configMapList = client.configMaps().inAnyNamespace().list();
        }

        List<ConfigMap> configMaps = configMapList.getItems();

        // 分页
        int total = configMaps.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<ConfigMap> pageList = start < total ? configMaps.subList(start, end) : List.of();

        List<ConfigMapVO> voList = pageList.stream()
                .map(this::convertToConfigMapVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public ConfigMapVO createConfigMap(ConfigMapDTO dto) {
        KubernetesClient client = clientFactory.getKubernetesClient(dto.getClusterId());

        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(dto.getName())
                .withNamespace(dto.getNamespace())
                .withLabels(dto.getData() != null ? new HashMap<>(dto.getData()) : null)
                .endMetadata()
                .withData(dto.getData())
                .withBinaryData(dto.getBinaryData())
                .withImmutable(dto.getImmutable())
                .build();

        ConfigMap created = client.configMaps()
                .inNamespace(dto.getNamespace())
                .create(configMap);

        return convertToConfigMapVO(created);
    }

    @Override
    public ConfigMapVO updateConfigMap(ConfigMapDTO dto) {
        KubernetesClient client = clientFactory.getKubernetesClient(dto.getClusterId());

        // 获取现有 ConfigMap
        ConfigMap existing = client.configMaps()
                .inNamespace(dto.getNamespace())
                .withName(dto.getName())
                .get();

        if (existing == null) {
            throw new CloudException("K8S_0003", "ConfigMap 不存在", HttpStatus.NOT_FOUND);
        }

        ConfigMap updated = new ConfigMapBuilder(existing)
                .withData(dto.getData())
                .withBinaryData(dto.getBinaryData())
                .build();

        ConfigMap result = client.configMaps()
                .inNamespace(dto.getNamespace())
                .withName(dto.getName())
                .replace(updated);

        return convertToConfigMapVO(result);
    }

    @Override
    public void deleteConfigMap(String clusterId, String namespace, String name) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        client.configMaps()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    @Override
    public PageResult<SecretVO> listSecrets(String clusterId, String namespace, int page, int size) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        SecretList secretList;
        if (namespace != null && !namespace.isEmpty()) {
            secretList = client.secrets().inNamespace(namespace).list();
        } else {
            secretList = client.secrets().inAnyNamespace().list();
        }

        List<Secret> secrets = secretList.getItems();

        // 分页
        int total = secrets.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Secret> pageList = start < total ? secrets.subList(start, end) : List.of();

        List<SecretVO> voList = pageList.stream()
                .map(this::convertToSecretVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public SecretVO createSecret(SecretDTO dto) {
        KubernetesClient client = clientFactory.getKubernetesClient(dto.getClusterId());

        Secret secret = new SecretBuilder()
                .withNewMetadata()
                .withName(dto.getName())
                .withNamespace(dto.getNamespace())
                .endMetadata()
                .withType(dto.getType())
                .withStringData(dto.getStringData())
                .withImmutable(dto.getImmutable())
                .build();

        Secret created = client.secrets()
                .inNamespace(dto.getNamespace())
                .create(secret);

        return convertToSecretVO(created);
    }

    @Override
    public void deleteSecret(String clusterId, String namespace, String name) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        client.secrets()
                .inNamespace(namespace)
                .withName(name)
                .delete();
    }

    @Override
    public List<NodeVO> listNodes(String clusterId) {
        KubernetesClient client = clientFactory.getKubernetesClient(clusterId);

        NodeList nodeList = client.nodes().list();

        return nodeList.getItems().stream()
                .map(this::convertToNodeVO)
                .collect(Collectors.toList());
    }

    // --- VO 转换方法 ---

    private NamespaceVO convertToNamespaceVO(Namespace namespace) {
        NamespaceVO vo = new NamespaceVO();
        vo.setName(namespace.getMetadata().getName());
        vo.setUid(namespace.getMetadata().getUid());
        vo.setStatus(namespace.getStatus().getPhase());
        vo.setLabels(namespace.getMetadata().getLabels());
        vo.setAnnotations(namespace.getMetadata().getAnnotations());
        vo.setCreationTimestamp(namespace.getMetadata().getCreationTimestamp());
        return vo;
    }

    private DeploymentVO convertToDeploymentVO(Deployment deployment) {
        DeploymentVO vo = new DeploymentVO();
        vo.setName(deployment.getMetadata().getName());
        vo.setNamespace(deployment.getMetadata().getNamespace());
        vo.setCreationTimestamp(deployment.getMetadata().getCreationTimestamp());

        DeploymentSpec spec = deployment.getSpec();
        if (spec != null) {
            vo.setReplicas(spec.getReplicas());
            vo.setStrategy(spec.getStrategy() != null ? spec.getStrategy().getType() : "");
        }

        DeploymentStatus status = deployment.getStatus();
        if (status != null) {
            vo.setAvailableReplicas(status.getAvailableReplicas());
            vo.setReadyReplicas(status.getReadyReplicas());
            vo.setUpdatedReplicas(status.getUpdatedReplicas());
            vo.setStatus(status.getConditions().stream()
                    .map(c -> c.getType() + ":" + c.getStatus())
                    .collect(Collectors.joining(", ")));
        }

        // 提取容器信息
        List<DeploymentVO.ContainerVO> containers = new ArrayList<>();
        if (spec != null && spec.getTemplate() != null && spec.getTemplate().getSpec() != null) {
            for (Container container : spec.getTemplate().getSpec().getContainers()) {
                DeploymentVO.ContainerVO cvo = new DeploymentVO.ContainerVO();
                cvo.setName(container.getName());
                cvo.setImage(container.getImage());
                cvo.setPorts(container.getPort() != null ? List.of(container.getPort().toString()) : List.of());
                containers.add(cvo);
            }
        }
        vo.setContainers(containers);

        // 标签和选择器
        vo.setLabels(deployment.getMetadata().getLabels());
        if (spec != null && spec.getSelector() != null && spec.getSelector().getMatchLabels() != null) {
            vo.setSelectors(new HashMap<>(spec.getSelector().getMatchLabels()));
        }

        return vo;
    }

    private PodVO convertToPodVO(Pod pod) {
        PodVO vo = new PodVO();
        vo.setName(pod.getMetadata().getName());
        vo.setNamespace(pod.getMetadata().getNamespace());
        vo.setCreationTimestamp(pod.getMetadata().getCreationTimestamp());

        PodStatus status = pod.getStatus();
        if (status != null) {
            vo.setStatus(status.getPhase());
            vo.setPhase(status.getPhase());
            vo.setPodIp(status.getPodIP());
            vo.setHostIp(status.getHostIP());
            vo.setRestartCount(status.getContainerStatuses().stream()
                    .map(ContainerStatus::getRestartCount)
                    .reduce(0, Integer::sum));

            // 提取容器状态
            if (status.getContainerStatuses() != null) {
                vo.setContainerStatuses(status.getContainerStatuses().stream()
                        .map(this::convertToContainerStatusVO)
                        .collect(Collectors.toList()));
            }
        }

        PodSpec spec = pod.getSpec();
        if (spec != null) {
            vo.setNodeName(spec.getNodeName());
            // 提取第一个容器的镜像
            if (!spec.getContainers().isEmpty()) {
                vo.setImage(spec.getContainers().get(0).getImage());
            }
        }

        vo.setLabels(pod.getMetadata().getLabels());

        return vo;
    }

    private PodVO.ContainerStatusVO convertToContainerStatusVO(ContainerStatus containerStatus) {
        PodVO.ContainerStatusVO vo = new PodVO.ContainerStatusVO();
        vo.setName(containerStatus.getName());
        vo.setReady(containerStatus.getReady());
        vo.setRestartCount(containerStatus.getRestartCount());

        ContainerState state = containerStatus.getState();
        if (state != null) {
            if (state.getRunning() != null) {
                vo.setState("Running");
            } else if (state.getWaiting() != null) {
                vo.setState("Waiting");
                vo.setReason(state.getWaiting().getReason());
                vo.setMessage(state.getWaiting().getMessage());
            } else if (state.getTerminated() != null) {
                vo.setState("Terminated");
                vo.setReason(state.getTerminated().getReason());
                vo.setMessage(state.getTerminated().getMessage());
            }
        }

        return vo;
    }

    private ServiceVO convertToServiceVO(Service service) {
        ServiceVO vo = new ServiceVO();
        vo.setName(service.getMetadata().getName());
        vo.setNamespace(service.getMetadata().getNamespace());
        vo.setCreationTimestamp(service.getMetadata().getCreationTimestamp());

        ServiceSpec spec = service.getSpec();
        if (spec != null) {
            vo.setType(spec.getType());
            vo.setClusterIp(spec.getClusterIP());
            if (spec.getPorts() != null) {
                vo.setPorts(spec.getPorts().stream()
                        .map(p -> p.getName() + ":" + p.getPort() + "/" + p.getTargetPort().getIntVal())
                        .collect(Collectors.toList()));
            }
        }

        ServiceStatus status = service.getStatus();
        if (status != null && status.getLoadBalancer() != null && status.getLoadBalancer().getIngress() != null) {
            vo.setLoadBalancerIp(status.getLoadBalancer().getIngress().stream()
                    .map(LoadBalancerIngress::getIp)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
        }

        vo.setLabels(service.getMetadata().getLabels());

        return vo;
    }

    private ConfigMapVO convertToConfigMapVO(ConfigMap configMap) {
        ConfigMapVO vo = new ConfigMapVO();
        vo.setName(configMap.getMetadata().getName());
        vo.setNamespace(configMap.getMetadata().getNamespace());
        vo.setCreationTimestamp(configMap.getMetadata().getCreationTimestamp());
        vo.setData(configMap.getData());
        vo.setBinaryData(configMap.getBinaryData());
        vo.setImmutable(configMap.getImmutable());
        vo.setLabels(configMap.getMetadata().getLabels());
        return vo;
    }

    private SecretVO convertToSecretVO(Secret secret) {
        SecretVO vo = new SecretVO();
        vo.setName(secret.getMetadata().getName());
        vo.setNamespace(secret.getMetadata().getNamespace());
        vo.setType(secret.getType());
        vo.setCreationTimestamp(secret.getMetadata().getCreationTimestamp());

        // 脱敏：不返回真实数据，只返回是否存在
        Map<String, String> stringData = new HashMap<>();
        if (secret.getStringData() != null) {
            secret.getStringData().forEach((k, v) -> stringData.put(k, v != null ? "***" : null));
        }
        vo.setStringData(stringData);

        vo.setImmutable(secret.getImmutable());
        vo.setLabels(secret.getMetadata().getLabels());

        return vo;
    }

    private NodeVO convertToNodeVO(Node node) {
        NodeVO vo = new NodeVO();
        vo.setName(node.getMetadata().getName());
        vo.setUid(node.getMetadata().getUid());
        vo.setCreationTimestamp(node.getMetadata().getCreationTimestamp());

        NodeStatus status = node.getStatus();
        if (status != null) {
            vo.setPhase(status.getPhase());
            vo.setNodeInfo(status.getNodeInfo());

            // 条件
            if (status.getConditions() != null) {
                vo.setConditions(status.getConditions().stream()
                        .map(c -> c.getType() + ":" + c.getStatus() + "(" + c.getReason() + ")")
                        .collect(Collectors.toList()));
            }

            // 资源
            if (status.getAllocatable() != null) {
                vo.setAllocatable(new HashMap<>(status.getAllocatable()));
            }
            if (status.getCapacity() != null) {
                vo.setCapacity(new HashMap<>(status.getCapacity()));
            }
        }

        vo.setLabels(node.getMetadata().getLabels());

        return vo;
    }
}
