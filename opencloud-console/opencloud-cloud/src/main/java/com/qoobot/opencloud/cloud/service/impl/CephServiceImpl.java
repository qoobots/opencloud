package com.qoobot.opencloud.cloud.service.impl;

import com.qoobot.opencloud.cloud.client.CloudClientFactory;
import com.qoobot.opencloud.cloud.domain.vo.CephOverviewVO;
import com.qoobot.opencloud.cloud.domain.vo.CephPoolVO;
import com.qoobot.opencloud.cloud.domain.vo.S3BucketVO;
import com.qoobot.opencloud.cloud.domain.vo.S3ObjectVO;
import com.qoobot.opencloud.cloud.exception.CloudException;
import com.qoobot.opencloud.cloud.service.CephService;
import com.qoobot.opencloud.common.core.page.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ceph 存储服务实现
 */
@Slf4j
@Service
public class CephServiceImpl implements CephService {

    @Autowired
    private CloudClientFactory clientFactory;

    private static final String MGR_API_BASE_PATH = "/api/v0";

    @Override
    public CephOverviewVO getOverview(String clusterId) {
        // 获取集群配置
        var config = clientFactory.getClusterConfig(clusterId);
        if (config == null) {
            throw new CloudException("CEPH_0012", "集群配置不存在");
        }

        CephOverviewVO vo = new CephOverviewVO();
        vo.setClusterId(clusterId);

        try {
            WebClient client = WebClient.builder()
                    .baseUrl(config.getEndpoint())
                    .build();

            // 获取集群状态
            String status = client.get()
                    .uri(MGR_API_BASE_PATH + "/status")
                    .retrieve()
                    .toEntity(String.class)
                    .block()
                    .getBody();

            // 简化处理，实际需要解析 JSON
            vo.setHealthStatus("HEALTH_OK");
            vo.setHealthMessage("Cluster is healthy");

            // 获取集群总览
            String df = client.get()
                    .uri(MGR_API_BASE_PATH + "/df")
                    .retrieve()
                    .toEntity(String.class)
                    .block()
                    .getBody();

            // 解析 df 结果（简化处理）
            vo.setTotalBytes(1000000000000L); // 1TB 示例
            vo.setUsedBytes(500000000000L);   // 500GB 示例
            vo.setAvailableBytes(500000000000L);
            vo.setUsagePercent(50.0);
            vo.setTotalObjects(1000);
            vo.setPoolCount(5);
            vo.setOsdCount(12);
            vo.setOsdUpCount(12);
            vo.setOsdInCount(12);

            // 获取 OSD 列表
            vo.setOsdStatuses(listOsds(clusterId));

        } catch (Exception e) {
            log.error("Failed to get Ceph overview for cluster {}", clusterId, e);
            vo.setHealthStatus("HEALTH_ERROR");
            vo.setHealthMessage("Failed to get overview: " + e.getMessage());
        }

        return vo;
    }

    @Override
    public List<CephPoolVO> listPools(String clusterId) {
        try {
            // 获取集群配置
            var config = clientFactory.getClusterConfig(clusterId);
            if (config == null) {
                throw new CloudException("CEPH_0012", "集群配置不存在");
            }

            WebClient client = WebClient.builder()
                    .baseUrl(config.getEndpoint())
                    .build();

            // 获取存储池列表
            String pools = client.get()
                    .uri(MGR_API_BASE_PATH + "/osd/pool/ls")
                    .retrieve()
                    .toEntity(String.class)
                    .block()
                    .getBody();

            // 简化处理，返回空列表
            // 实际需要解析 JSON 并获取详细信息
            return List.of();

        } catch (Exception e) {
            log.error("Failed to list pools for cluster {}", clusterId, e);
            throw new CloudException("CEPH_0001", "获取存储池列表失败: " + e.getMessage());
        }
    }

    @Override
    public CephPoolVO getPool(String clusterId, String poolName) {
        try {
            // 获取集群配置
            var config = clientFactory.getClusterConfig(clusterId);
            if (config == null) {
                throw new CloudException("CEPH_0012", "集群配置不存在");
            }

            WebClient client = WebClient.builder()
                    .baseUrl(config.getEndpoint())
                    .build();

            // 获取存储池详细信息
            String detail = client.get()
                    .uri(MGR_API_BASE_PATH + "/osd/pool/stats?pool=" + poolName)
                    .retrieve()
                    .toEntity(String.class)
                    .block()
                    .getBody();

            // 简化处理
            return null;

        } catch (Exception e) {
            log.error("Failed to get pool {} for cluster {}", poolName, clusterId, e);
            throw new CloudException("CEPH_0013", "获取存储池详情失败: " + e.getMessage());
        }
    }

    @Override
    public List<CephOverviewVO.OsdStatus> listOsds(String clusterId) {
        try {
            // 获取集群配置
            var config = clientFactory.getClusterConfig(clusterId);
            if (config == null) {
                throw new CloudException("CEPH_0012", "集群配置不存在");
            }

            WebClient client = WebClient.builder()
                    .baseUrl(config.getEndpoint())
                    .build();

            // 获取 OSD 列表
            String osds = client.get()
                    .uri(MGR_API_BASE_PATH + "/osd/ls")
                    .retrieve()
                    .toEntity(String.class)
                    .block()
                    .getBody();

            // 简化处理，返回空列表
            // 实际需要解析 JSON 并获取每个 OSD 的详细状态
            return List.of();

        } catch (Exception e) {
            log.error("Failed to list OSDs for cluster {}", clusterId, e);
            throw new CloudException("CEPH_0014", "获取 OSD 列表失败: " + e.getMessage());
        }
    }

    @Override
    public List<S3BucketVO> listBuckets(String clusterId) {
        S3Client s3Client = clientFactory.getCephRgwClient(clusterId);

        try {
            ListBucketsResponse response = s3Client.listBuckets();

            return response.buckets().stream()
                    .map(bucket -> {
                        S3BucketVO vo = new S3BucketVO();
                        vo.setName(bucket.name());
                        vo.setCreationDate(bucket.creationDate());
                        return vo;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to list buckets for cluster {}", clusterId, e);
            throw new CloudException("CEPH_0001", "获取 Bucket 列表失败: " + e.getMessage());
        }
    }

    @Override
    public void createBucket(String clusterId, String bucketName) {
        S3Client s3Client = clientFactory.getCephRgwClient(clusterId);

        try {
            CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.createBucket(request);
        } catch (BucketAlreadyExistsException e) {
            throw new CloudException("CEPH_0002", "Bucket 已存在");
        } catch (Exception e) {
            log.error("Failed to create bucket {} in cluster {}", bucketName, clusterId, e);
            throw new CloudException("CEPH_0003", "创建 Bucket 失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteBucket(String clusterId, String bucketName) {
        S3Client s3Client = clientFactory.getCephRgwClient(clusterId);

        try {
            // 先检查 Bucket 是否为空
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(1)
                    .build();
            ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);

            if (!listResponse.contents().isEmpty()) {
                throw new CloudException("CEPH_0004", "Bucket 不为空，无法删除");
            }

            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            s3Client.deleteBucket(request);
        } catch (NoSuchBucketException e) {
            throw new CloudException("CEPH_0005", "Bucket 不存在", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to delete bucket {} in cluster {}", bucketName, clusterId, e);
            throw new CloudException("CEPH_0006", "删除 Bucket 失败: " + e.getMessage());
        }
    }

    @Override
    public PageResult<S3ObjectVO> listObjects(String clusterId, String bucketName, String prefix, int page, int size) {
        S3Client s3Client = clientFactory.getCephRgwClient(clusterId);

        try {
            // RGW S3 不支持真正的分页，需要遍历所有对象
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(1000);

            if (prefix != null && !prefix.isEmpty()) {
                requestBuilder.prefix(prefix);
            }

            List<S3Object> allObjects = new ArrayList<>();
            String continuationToken = null;

            do {
                if (continuationToken != null) {
                    requestBuilder.continuationToken(continuationToken);
                }

                ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());
                allObjects.addAll(response.contents());
                continuationToken = response.nextContinuationToken();
            } while (continuationToken != null);

            // 内存分页
            int total = allObjects.size();
            int start = (page - 1) * size;
            int end = Math.min(start + size, total);
            List<S3Object> pageList = start < total ? allObjects.subList(start, end) : List.of();

            List<S3ObjectVO> voList = pageList.stream()
                    .map(this::convertToObjectVO)
                    .collect(Collectors.toList());

            return new PageResult<>(voList, total, page, size);

        } catch (NoSuchBucketException e) {
            throw new CloudException("CEPH_0005", "Bucket 不存在", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to list objects in bucket {} cluster {}", bucketName, clusterId, e);
            throw new CloudException("CEPH_0007", "获取对象列表失败: " + e.getMessage());
        }
    }

    @Override
    public void uploadObject(String clusterId, String bucketName, String key, InputStream inputStream, long size, String contentType) {
        S3Client s3Client = clientFactory.getCephRgwClient(clusterId);

        try {
            PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key);

            if (contentType != null) {
                requestBuilder.contentType(contentType);
            }

            s3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(inputStream, size));
        } catch (Exception e) {
            log.error("Failed to upload object {}/{} in cluster {}", bucketName, key, clusterId, e);
            throw new CloudException("CEPH_0008", "上传对象失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream downloadObject(String clusterId, String bucketName, String key) {
        S3Client s3Client = clientFactory.getCephRgwClient(clusterId);

        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request);
            return response;
        } catch (NoSuchKeyException e) {
            throw new CloudException("CEPH_0009", "对象不存在", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            log.error("Failed to download object {}/{} in cluster {}", bucketName, key, clusterId, e);
            throw new CloudException("CEPH_0010", "下载对象失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteObject(String clusterId, String bucketName, String key) {
        S3Client s3Client = clientFactory.getCephRgwClient(clusterId);

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(request);
        } catch (Exception e) {
            log.error("Failed to delete object {}/{} in cluster {}", bucketName, key, clusterId, e);
            throw new CloudException("CEPH_0011", "删除对象失败: " + e.getMessage());
        }
    }

    private S3ObjectVO convertToObjectVO(S3Object object) {
        S3ObjectVO vo = new S3ObjectVO();
        vo.setKey(object.key());
        vo.setSize(object.size());
        vo.setLastModified(object.lastModified());
        vo.setEtag(object.eTag());
        vo.setStorageClass(object.storageClassAsString());
        vo.setIsDirectory(object.key().endsWith("/"));
        return vo;
    }
}
