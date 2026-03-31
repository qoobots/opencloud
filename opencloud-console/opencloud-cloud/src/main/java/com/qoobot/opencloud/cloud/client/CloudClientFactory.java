package com.qoobot.opencloud.cloud.client;

import com.qoobot.opencloud.cloud.config.EncryptUtil;
import com.qoobot.opencloud.cloud.domain.entity.ClusterConfig;
import com.qoobot.opencloud.cloud.exception.CloudException;
import com.qoobot.opencloud.cloud.mapper.ClusterConfigMapper;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.openstack.OSFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 云平台客户端工厂
 * 按 clusterId 缓存客户端实例
 */
@Component
public class CloudClientFactory {

    private final ConcurrentHashMap<String, OSClient.OSClientV3> openStackClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, S3Client> cephRgwClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, KubernetesClient> k8sClients = new ConcurrentHashMap<>();

    @Autowired
    private ClusterConfigMapper clusterConfigMapper;

    @Autowired
    private EncryptUtil encryptUtil;

    /**
     * 获取 OpenStack 客户端
     */
    public OSClient.OSClientV3 getOpenStackClient(String clusterId) {
        return openStackClients.computeIfAbsent(clusterId, id -> {
            ClusterConfig config = getAndDecryptConfig(id);
            return createOpenStackClient(config);
        });
    }

    /**
     * 获取 Ceph RGW S3 客户端
     */
    public S3Client getCephRgwClient(String clusterId) {
        return cephRgwClients.computeIfAbsent(clusterId, id -> {
            ClusterConfig config = getAndDecryptConfig(id);
            return createCephRgwClient(config);
        });
    }

    /**
     * 获取 Kubernetes 客户端
     */
    public KubernetesClient getKubernetesClient(String clusterId) {
        return k8sClients.computeIfAbsent(clusterId, id -> {
            ClusterConfig config = getAndDecryptConfig(id);
            return createKubernetesClient(config);
        });
    }

    /**
     * 清除指定集群的客户端缓存
     */
    public void invalidateClient(String clusterId) {
        openStackClients.remove(clusterId);
        
        S3Client s3Client = cephRgwClients.remove(clusterId);
        if (s3Client != null) {
            s3Client.close();
        }

        KubernetesClient k8sClient = k8sClients.remove(clusterId);
        if (k8sClient != null) {
            k8sClient.close();
        }
    }

    /**
     * 获取并解密集群配置
     */
    private ClusterConfig getAndDecryptConfig(String clusterId) {
        ClusterConfig config = clusterConfigMapper.selectById(clusterId);
        if (config == null) {
            throw new CloudException("CLOUD_0001", "集群配置不存在", HttpStatus.NOT_FOUND);
        }
        // 解密敏感字段
        String decryptedJson = encryptUtil.decrypt(config.getConfigJson());
        config.setConfigJson(decryptedJson);
        return config;
    }

    /**
     * 获取集群配置（不解密，用于 Ceph MGR API 连接测试）
     */
    public ClusterConfig getClusterConfig(String clusterId) {
        return clusterConfigMapper.selectById(clusterId);
    }

    /**
     * 创建 OpenStack 客户端
     */
    @SuppressWarnings("unchecked")
    private OSClient.OSClientV3 createOpenStackClient(ClusterConfig config) {
        try {
            Map<String, Object> cfg = parseConfig(config.getConfigJson());
            String username = (String) cfg.get("username");
            String password = (String) cfg.get("password");
            String projectName = (String) cfg.get("projectName");
            String domainName = (String) cfg.getOrDefault("domainName", "Default");

            return OSFactory.builderV3()
                    .endpoint(config.getEndpoint())
                    .credentials(username, password, Identifier.byName(domainName))
                    .scopeToProject(Identifier.byName(projectName), Identifier.byName(domainName))
                    .authenticate();
        } catch (Exception e) {
            throw new CloudException("CLOUD_0003", "OpenStack 认证失败: " + e.getMessage());
        }
    }

    /**
     * 创建 Ceph RGW S3 客户端
     */
    @SuppressWarnings("unchecked")
    private S3Client createCephRgwClient(ClusterConfig config) {
        try {
            Map<String, Object> cfg = parseConfig(config.getConfigJson());
            String rgwEndpoint = (String) cfg.get("rgwEndpoint");
            String accessKey = (String) cfg.get("accessKey");
            String secretKey = (String) cfg.get("secretKey");

            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

            return S3Client.builder()
                    .endpointOverride(URI.create(rgwEndpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .region(Region.of("us-east-1"))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build())
                    .build();
        } catch (Exception e) {
            throw new CloudException("CEPH_0004", "RGW 客户端创建失败: " + e.getMessage());
        }
    }

    /**
     * 创建 Kubernetes 客户端
     */
    @SuppressWarnings("unchecked")
    private KubernetesClient createKubernetesClient(ClusterConfig config) {
        try {
            Map<String, Object> cfg = parseConfig(config.getConfigJson());
            String connectionType = (String) cfg.get("connectionType");

            Config fabric8Config;

            if ("IN_CLUSTER".equals(connectionType)) {
                fabric8Config = Config.autoConfigure(null);
            } else {
                String kubeconfigContent = (String) cfg.get("kubeconfigContent");
                if (kubeconfigContent == null || kubeconfigContent.isEmpty()) {
                    throw new CloudException("K8S_0005", "kubeconfig 内容为空");
                }
                String kubeconfig = new String(
                        Base64.getDecoder().decode(kubeconfigContent),
                        StandardCharsets.UTF_8
                );
                fabric8Config = Config.fromKubeconfig(kubeconfig);
            }

            return new KubernetesClientBuilder()
                    .withConfig(fabric8Config)
                    .build();
        } catch (Exception e) {
            throw new CloudException("K8S_0005", "Kubernetes 客户端创建失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseConfig(String json) {
        // 简化实现，实际使用 JSON 库解析
        // 这里假设传入的是 Map 的 toString 格式或 JSON 格式
        return new java.util.HashMap<>();
    }
}
