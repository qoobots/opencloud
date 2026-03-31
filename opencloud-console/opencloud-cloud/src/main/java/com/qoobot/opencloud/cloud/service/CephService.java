package com.qoobot.opencloud.cloud.service;

import com.qoobot.opencloud.cloud.domain.vo.CephOverviewVO;
import com.qoobot.opencloud.cloud.domain.vo.CephPoolVO;
import com.qoobot.opencloud.cloud.domain.vo.S3BucketVO;
import com.qoobot.opencloud.cloud.domain.vo.S3ObjectVO;
import com.qoobot.opencloud.common.core.page.PageResult;

import java.io.InputStream;
import java.util.List;

/**
 * Ceph 存储服务接口
 */
public interface CephService {

    /**
     * 获取集群总览
     */
    CephOverviewVO getOverview(String clusterId);

    /**
     * 获取存储池列表
     */
    List<CephPoolVO> listPools(String clusterId);

    /**
     * 获取存储池详情
     */
    CephPoolVO getPool(String clusterId, String poolName);

    /**
     * 获取 OSD 列表
     */
    List<CephOverviewVO.OsdStatus> listOsds(String clusterId);

    /**
     * 获取 Bucket 列表
     */
    List<S3BucketVO> listBuckets(String clusterId);

    /**
     * 创建 Bucket
     */
    void createBucket(String clusterId, String bucketName);

    /**
     * 删除 Bucket
     */
    void deleteBucket(String clusterId, String bucketName);

    /**
     * 获取对象列表
     */
    PageResult<S3ObjectVO> listObjects(String clusterId, String bucketName, String prefix, int page, int size);

    /**
     * 上传对象
     */
    void uploadObject(String clusterId, String bucketName, String key, InputStream inputStream, long size, String contentType);

    /**
     * 下载对象
     */
    InputStream downloadObject(String clusterId, String bucketName, String key);

    /**
     * 删除对象
     */
    void deleteObject(String clusterId, String bucketName, String key);
}
