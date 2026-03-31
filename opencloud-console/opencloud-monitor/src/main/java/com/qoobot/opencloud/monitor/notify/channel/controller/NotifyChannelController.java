package com.qoobot.opencloud.monitor.notify.channel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qoobot.opencloud.common.domain.R;
import com.qoobot.opencloud.monitor.notify.channel.domain.dto.NotifyChannelCreateDTO;
import com.qoobot.opencloud.monitor.notify.channel.domain.dto.NotifyChannelQueryDTO;
import com.qoobot.opencloud.monitor.notify.channel.domain.vo.NotifyChannelVO;
import com.qoobot.opencloud.monitor.notify.channel.service.NotifyChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 通知渠道控制器
 */
@RestController
@RequestMapping("/api/monitor/notify/channels")
@RequiredArgsConstructor
@Validated
public class NotifyChannelController {

    private final NotifyChannelService notifyChannelService;

    @GetMapping
    @PreAuthorize("hasAuthority('monitor:notify:channel:list')")
    public R<IPage<NotifyChannelVO>> list(NotifyChannelQueryDTO queryDTO) {
        return R.ok(notifyChannelService.listChannels(queryDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:notify:channel:list')")
    public R<NotifyChannelVO> getById(@PathVariable Long id) {
        return R.ok(notifyChannelService.getChannelById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('monitor:notify:channel:add')")
    public R<NotifyChannelVO> create(@RequestBody @Validated NotifyChannelCreateDTO createDTO) {
        return R.ok(notifyChannelService.createChannel(createDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:notify:channel:edit')")
    public R<NotifyChannelVO> update(@PathVariable Long id,
                                      @RequestBody @Validated NotifyChannelCreateDTO updateDTO) {
        return R.ok(notifyChannelService.updateChannel(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:notify:channel:delete')")
    public R<Void> delete(@PathVariable Long id) {
        notifyChannelService.deleteChannel(id);
        return R.ok();
    }

    @PostMapping("/{id}/test")
    @PreAuthorize("hasAuthority('monitor:notify:channel:edit')")
    public R<Map<String, Object>> test(@PathVariable Long id) {
        return R.ok(notifyChannelService.testChannel(id));
    }
}
