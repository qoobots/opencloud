package com.qoobot.opencloud.cloud.service.impl;

import com.qoobot.opencloud.cloud.client.CloudClientFactory;
import com.qoobot.opencloud.cloud.domain.dto.InstanceActionDTO;
import com.qoobot.opencloud.cloud.domain.dto.InstanceCreateDTO;
import com.qoobot.opencloud.cloud.domain.vo.FlavorVO;
import com.qoobot.opencloud.cloud.domain.vo.InstanceVO;
import com.qoobot.opencloud.cloud.domain.vo.KeyPairVO;
import com.qoobot.opencloud.cloud.exception.CloudException;
import com.qoobot.opencloud.cloud.service.CloudQuotaService;
import com.qoobot.opencloud.cloud.service.OpenStackService;
import com.qoobot.opencloud.cloud.service.TaskService;
import com.qoobot.opencloud.common.core.page.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.*;
import org.openstack4j.model.compute.builder.ServerCreateBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenStack 服务实现
 */
@Slf4j
@Service
public class OpenStackServiceImpl implements OpenStackService {

    @Autowired
    private CloudClientFactory clientFactory;

    @Autowired
    private TaskService taskService;

    @Autowired
    private CloudQuotaService quotaService;

    @Override
    public PageResult<InstanceVO> listInstances(String clusterId, String status, String name, int page, int size) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        List<? extends Server> servers = os.compute().servers().list(true);

        // 过滤
        List<Server> filtered = servers.stream()
                .filter(s -> status == null || s.getStatus().name().equalsIgnoreCase(status))
                .filter(s -> name == null || s.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());

        // 分页
        int total = filtered.size();
        int start = (page - 1) * size;
        int end = Math.min(start + size, total);
        List<Server> pageList = start < total ? filtered.subList(start, end) : new ArrayList<>();

        List<InstanceVO> voList = pageList.stream()
                .map(this::convertToInstanceVO)
                .collect(Collectors.toList());

        return new PageResult<>(voList, total, page, size);
    }

    @Override
    public InstanceVO getInstance(String clusterId, String instanceId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        Server server = os.compute().servers().get(instanceId);
        if (server == null) {
            throw new CloudException("OS_0001", "云主机不存在", HttpStatus.NOT_FOUND);
        }
        return convertToInstanceVO(server);
    }

    @Override
    public String createInstance(InstanceCreateDTO dto) {
        // 配额检查
        quotaService.checkAndReserveQuota(dto.getClusterId(), dto);

        // 创建异步任务
        return taskService.submitTask("INSTANCE_CREATE", dto.getClusterId(), dto);
    }

    @Override
    public void performAction(String clusterId, String instanceId, InstanceActionDTO dto) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        Server server = os.compute().servers().get(instanceId);
        if (server == null) {
            throw new CloudException("OS_0001", "云主机不存在", HttpStatus.NOT_FOUND);
        }

        switch (dto.getAction()) {
            case "start":
                os.compute().servers().action(instanceId, Action.START);
                break;
            case "stop":
                os.compute().servers().action(instanceId, Action.STOP);
                break;
            case "reboot":
                os.compute().servers().reboot(instanceId, RebootType.SOFT);
                break;
            case "hardReboot":
                os.compute().servers().reboot(instanceId, RebootType.HARD);
                break;
            case "pause":
                os.compute().servers().action(instanceId, Action.PAUSE);
                break;
            case "unpause":
                os.compute().servers().action(instanceId, Action.UNPAUSE);
                break;
            case "suspend":
                os.compute().servers().action(instanceId, Action.SUSPEND);
                break;
            case "resume":
                os.compute().servers().action(instanceId, Action.RESUME);
                break;
            default:
                throw new CloudException("OS_0004", "不支持的操作类型: " + dto.getAction());
        }
    }

    @Override
    public void deleteInstance(String clusterId, String instanceId, boolean force) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        Server server = os.compute().servers().get(instanceId);
        if (server == null) {
            throw new CloudException("OS_0001", "云主机不存在", HttpStatus.NOT_FOUND);
        }

        if (force) {
            os.compute().servers().delete(instanceId);
        } else {
            // 正常删除需要云主机处于 SHUTOFF 或 ERROR 状态
            if (server.getStatus() != Server.Status.SHUTOFF && server.getStatus() != Server.Status.ERROR) {
                throw new CloudException("OS_0005", "云主机未关机，无法删除");
            }
            os.compute().servers().delete(instanceId);
        }

        // 释放配额
        quotaService.releaseQuota(clusterId, server.getFlavorId());
    }

    @Override
    public List<FlavorVO> listFlavors(String clusterId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        List<? extends Flavor> flavors = os.compute().flavors().list();

        return flavors.stream()
                .filter(f -> f.isPublic() || f.getTenantId() == null)
                .map(this::convertToFlavorVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<KeyPairVO> listKeyPairs(String clusterId) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        List<? extends Keypair> keypairs = os.compute().keypairs().list();

        return keypairs.stream()
                .map(kp -> {
                    KeyPairVO vo = new KeyPairVO();
                    vo.setName(kp.getName());
                    vo.setFingerprint(kp.getFingerprint());
                    vo.setPublicKey(kp.getPublicKey());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    public KeyPairVO createKeyPair(String clusterId, String name, String publicKey) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);

        Keypair kp;
        if (publicKey != null && !publicKey.isEmpty()) {
            kp = os.compute().keypairs().create(name, publicKey);
        } else {
            kp = os.compute().keypairs().create(name);
        }

        KeyPairVO vo = new KeyPairVO();
        vo.setName(kp.getName());
        vo.setFingerprint(kp.getFingerprint());
        vo.setPublicKey(kp.getPublicKey());
        vo.setPrivateKey(kp.getPrivateKey());
        return vo;
    }

    @Override
    public void deleteKeyPair(String clusterId, String name) {
        OSClient.OSClientV3 os = clientFactory.getOpenStackClient(clusterId);
        os.compute().keypairs().delete(name);
    }

    private InstanceVO convertToInstanceVO(Server server) {
        InstanceVO vo = new InstanceVO();
        vo.setId(server.getId());
        vo.setName(server.getName());
        vo.setStatus(server.getStatus().name());
        vo.setPowerState(server.getPowerState());
        vo.setTenantId(server.getTenantId());
        vo.setHostId(server.getHost());
        vo.setAvailabilityZone(server.getAvailabilityZone());
        vo.setCreatedAt(server.getCreated());
        vo.setUpdatedAt(server.getUpdated());
        vo.setKeyPairName(server.getKeyName());

        if (server.getFlavor() != null) {
            vo.setFlavorId(server.getFlavor().getId());
            vo.setFlavorName(server.getFlavor().getName());
        }

        if (server.getImage() != null) {
            vo.setImageId(server.getImage().getId());
        }

        // 网络信息
        if (server.getAddresses() != null && server.getAddresses().getAddresses() != null) {
            List<InstanceVO.NetworkInfo> networks = new ArrayList<>();
            for (List<? extends Address> addrList : server.getAddresses().getAddresses().values()) {
                for (Address addr : addrList) {
                    InstanceVO.NetworkInfo netInfo = new InstanceVO.NetworkInfo();
                    netInfo.setMacAddress(addr.getMacAddr());
                    netInfo.setFixedIps(List.of(addr.getAddr()));
                    networks.add(netInfo);
                }
            }
            vo.setNetworks(networks);
        }

        return vo;
    }

    private FlavorVO convertToFlavorVO(Flavor flavor) {
        FlavorVO vo = new FlavorVO();
        vo.setId(flavor.getId());
        vo.setName(flavor.getName());
        vo.setVcpus(flavor.getVcpus());
        vo.setRam(flavor.getRam());
        vo.setDisk(flavor.getDisk());
        vo.setEphemeral(flavor.getEphemeral());
        vo.setSwap(flavor.getSwap());
        vo.setRxTxFactor(flavor.getRxTxFactor());
        vo.setIsPublic(flavor.isPublic());
        vo.setExtraSpecs(flavor.getExtraSpecs());
        return vo;
    }
}
