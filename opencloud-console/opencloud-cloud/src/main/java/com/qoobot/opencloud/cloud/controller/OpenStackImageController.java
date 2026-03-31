package com.qoobot.opencloud.cloud.controller;

import com.qoobot.opencloud.cloud.domain.vo.ImageVO;
import com.qoobot.opencloud.cloud.service.OpenStackImageService;
import com.qoobot.opencloud.common.core.page.PageResult;
import com.qoobot.opencloud.common.core.result.R;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * OpenStack 镜像管理控制器
 */
@Validated
@RestController
@RequestMapping("/api/cloud/openstack")
public class OpenStackImageController {

    @Autowired
    private OpenStackImageService imageService;

    /**
     * 镜像列表
     */
    @GetMapping("/images")
    public R<PageResult<ImageVO>> listImages(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(imageService.listImages(clusterId, visibility, name, page, size));
    }

    /**
     * 删除镜像
     */
    @DeleteMapping("/images/{id}")
    public R<Void> deleteImage(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String id) {
        imageService.deleteImage(clusterId, id);
        return R.ok();
    }

    /**
     * 上传镜像
     */
    @PostMapping("/images/upload")
    public R<ImageVO> uploadImage(
            @RequestParam @NotBlank String clusterId,
            @RequestParam @NotBlank String name,
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "bare") String containerFormat,
            @RequestParam(defaultValue = "qcow2") String diskFormat,
            @RequestParam(defaultValue = "0") int minDisk,
            @RequestParam(defaultValue = "0") int minRam,
            @RequestParam(defaultValue = "false") boolean isPublic) throws IOException {
        ImageVO imageVO = imageService.uploadImage(
                clusterId, name, file.getInputStream(), file.getSize(),
                containerFormat, diskFormat, minDisk, minRam, isPublic);
        return R.ok(imageVO);
    }
}
