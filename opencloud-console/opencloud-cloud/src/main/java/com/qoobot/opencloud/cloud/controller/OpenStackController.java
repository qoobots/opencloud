package com.qoobot.opencloud.cloud.controller;

import com.qoobot.opencloud.cloud.domain.dto.InstanceActionDTO;
import com.qoobot.opencloud.cloud.domain.dto.InstanceCreateDTO;
import com.qoobot.opencloud.cloud.domain.vo.FlavorVO;
import com.qoobot.opencloud.cloud.domain.vo.InstanceVO;
import com.qoobot.opencloud.cloud.domain.vo.KeyPairVO;
import com.qoobot.opencloud.cloud.service.OpenStackService;
import com.qoobot.opencloud.common.core.page.PageResult;
import com.qoobot.opencloud.common.core.result.R;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * OpenStack 资源管理控制器
 */
@Validated
@RestController
@RequestMapping("/api/cloud/openstack")
public class OpenStackController {

    @Autowired
    private OpenStackService openStackService;

    /**
     * 云主机列表
     */
    @GetMapping("/instances")
    public R<PageResult<InstanceVO>> listInstances(
            @RequestParam @NotBlank String clusterId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return R.ok(openStackService.listInstances(clusterId, status, name, page, size));
    }

    /**
     * 云主机详情
     */
    @GetMapping("/instances/{id}")
    public R<InstanceVO> getInstance(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String id) {
        return R.ok(openStackService.getInstance(clusterId, id));
    }

    /**
     * 创建云主机
     */
    @PostMapping("/instances")
    public R<String> createInstance(@Valid @RequestBody InstanceCreateDTO dto) {
        String taskId = openStackService.createInstance(dto);
        return R.ok(taskId);
    }

    /**
     * 云主机操作
     */
    @PutMapping("/instances/{id}/action")
    public R<Void> performAction(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String id,
            @Valid @RequestBody InstanceActionDTO dto) {
        openStackService.performAction(clusterId, id, dto);
        return R.ok();
    }

    /**
     * 删除云主机
     */
    @DeleteMapping("/instances/{id}")
    public R<Void> deleteInstance(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean force) {
        openStackService.deleteInstance(clusterId, id, force);
        return R.ok();
    }

    /**
     * 规格列表
     */
    @GetMapping("/flavors")
    public R<List<FlavorVO>> listFlavors(@RequestParam @NotBlank String clusterId) {
        return R.ok(openStackService.listFlavors(clusterId));
    }

    /**
     * 密钥对列表
     */
    @GetMapping("/keypairs")
    public R<List<KeyPairVO>> listKeyPairs(@RequestParam @NotBlank String clusterId) {
        return R.ok(openStackService.listKeyPairs(clusterId));
    }

    /**
     * 创建密钥对
     */
    @PostMapping("/keypairs")
    public R<KeyPairVO> createKeyPair(
            @RequestParam @NotBlank String clusterId,
            @RequestParam @NotBlank String name,
            @RequestParam(required = false) String publicKey) {
        return R.ok(openStackService.createKeyPair(clusterId, name, publicKey));
    }

    /**
     * 删除密钥对
     */
    @DeleteMapping("/keypairs/{name}")
    public R<Void> deleteKeyPair(
            @RequestParam @NotBlank String clusterId,
            @PathVariable String name) {
        openStackService.deleteKeyPair(clusterId, name);
        return R.ok();
    }
}
