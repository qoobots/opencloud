package com.qoobot.opencloud.cloud.controller;

import com.qoobot.opencloud.cloud.domain.vo.CephOverviewVO;
import com.qoobot.opencloud.cloud.domain.vo.CephPoolVO;
import com.qoobot.opencloud.cloud.domain.vo.S3BucketVO;
import com.qoobot.opencloud.cloud.domain.vo.S3ObjectVO;
import com.qoobot.opencloud.cloud.service.CephService;
import com.qoobot.opencloud.common.core.page.PageResult;
import com.qoobot.opencloud.common.core.result.R;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Ceph 存储管理控制器
 */
@Validated
@RestController
@RequestMapping("/api/cloud/ceph")
public class CephController {

    @Autowired
    private CephService cephService;

    /**
     * 集群总览
     */
    @GetMapping("/overview")
    public R<CephOverviewVO> getOverview(@RequestParam @NotBlank String clusterId) {
        return R.ok(cephService.getOverview(clusterId));
    }

    /**
     * 存储池列表
     */
    @GetMapping("/pools")
    public R<List<CephPoolVO>> listPools(@RequestParam @NotBlank String clusterId) {
        return R.ok(cephService.listPools(clusterId));
    }

    /**
     * 存储池详情
     */
    @GetMapping("/pools/{name}")
    public R<CephPoolVO> getPool(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String name) {
        return R.ok(cephService.getPool(clusterId, name));
    }

    /**
     * OSD 列表
     */
    @GetMapping("/osds")
    public R<List<CephOverviewVO.OsdStatus>> listOsds(@RequestParam @NotBlank String clusterId) {
        return R.ok(cephService.listOsds(clusterId));
    }

    /**
     * Bucket 列表
     */
    @GetMapping("/buckets")
    public R<List<S3BucketVO>> listBuckets(@RequestParam @NotBlank String clusterId) {
        return R.ok(cephService.listBuckets(clusterId));
    }

    /**
     * 创建 Bucket
     */
    @PostMapping("/buckets")
    public R<Void> createBucket(
            @RequestParam @NotBlank String clusterId,
            @RequestParam @NotBlank String bucketName) {
        cephService.createBucket(clusterId, bucketName);
        return R.ok();
    }

    /**
     * 删除 Bucket
     */
    @DeleteMapping("/buckets/{name}")
    public R<Void> deleteBucket(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String name) {
        cephService.deleteBucket(clusterId, name);
        return R.ok();
    }

    /**
     * 对象列表
     */
    @GetMapping("/buckets/{bucketName}/objects")
    public R<PageResult<S3ObjectVO>> listObjects(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String bucketName,
            @RequestParam(required = false) String prefix,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return R.ok(cephService.listObjects(clusterId, bucketName, prefix, page, size));
    }

    /**
     * 上传对象
     */
    @PostMapping("/buckets/{bucketName}/objects")
    public R<Void> uploadObject(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String bucketName,
            @RequestParam @NotBlank String key,
            @RequestParam MultipartFile file) throws IOException {
        cephService.uploadObject(clusterId, bucketName, key, file.getInputStream(), file.getSize(), file.getContentType());
        return R.ok();
    }

    /**
     * 下载对象
     */
    @GetMapping("/buckets/{bucketName}/objects/download")
    public void downloadObject(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String bucketName,
            @RequestParam @NotBlank String key,
            HttpServletResponse response) throws IOException {
        try (InputStream is = cephService.downloadObject(clusterId, bucketName, key)) {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + key + "\"");
            is.transferTo(response.getOutputStream());
        }
    }

    /**
     * 删除对象
     */
    @DeleteMapping("/buckets/{bucketName}/objects")
    public R<Void> deleteObject(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String bucketName,
            @RequestParam @NotBlank String key) {
        cephService.deleteObject(clusterId, bucketName, key);
        return R.ok();
    }
}
