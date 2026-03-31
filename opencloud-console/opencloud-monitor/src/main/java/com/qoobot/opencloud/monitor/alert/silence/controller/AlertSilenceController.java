package com.qoobot.opencloud.monitor.alert.silence.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qoobot.opencloud.common.model.R;
import com.qoobot.opencloud.monitor.alert.silence.domain.dto.AlertSilenceCreateDTO;
import com.qoobot.opencloud.monitor.alert.silence.domain.dto.AlertSilenceQueryDTO;
import com.qoobot.opencloud.monitor.alert.silence.domain.vo.AlertSilenceVO;
import com.qoobot.opencloud.monitor.alert.silence.service.AlertSilenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 告警静默 Controller
 */
@RestController
@RequestMapping("/api/monitor/alert/silences")
@RequiredArgsConstructor
public class AlertSilenceController {

    private final AlertSilenceService alertSilenceService;

    /**
     * 分页查询告警静默规则
     */
    @GetMapping
    @PreAuthorize("hasAuthority('monitor:alert:silence:list')")
    public R<IPage<AlertSilenceVO>> list(AlertSilenceQueryDTO queryDTO) {
        return R.ok(alertSilenceService.listSilences(queryDTO));
    }

    /**
     * 查询静默规则详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:alert:silence:list')")
    public R<AlertSilenceVO> detail(@PathVariable Long id) {
        return R.ok(alertSilenceService.getSilenceById(id));
    }

    /**
     * 创建告警静默规则
     */
    @PostMapping
    @PreAuthorize("hasAuthority('monitor:alert:silence:create')")
    public R<AlertSilenceVO> create(@RequestBody @Valid AlertSilenceCreateDTO createDTO) {
        return R.ok(alertSilenceService.createSilence(createDTO));
    }

    /**
     * 更新告警静默规则
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:alert:silence:edit')")
    public R<AlertSilenceVO> update(@PathVariable Long id,
                                     @RequestBody @Valid AlertSilenceCreateDTO updateDTO) {
        return R.ok(alertSilenceService.updateSilence(id, updateDTO));
    }

    /**
     * 停止静默（手动提前结束）
     */
    @PutMapping("/{id}/stop")
    @PreAuthorize("hasAuthority('monitor:alert:silence:edit')")
    public R<Void> stop(@PathVariable Long id) {
        alertSilenceService.stopSilence(id);
        return R.ok();
    }

    /**
     * 删除告警静默规则（仅允许删除已过期规则）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:alert:silence:delete')")
    public R<Void> delete(@PathVariable Long id) {
        alertSilenceService.deleteSilence(id);
        return R.ok();
    }
}
