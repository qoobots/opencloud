package com.qoobot.opencloud.monitor.alert.rule.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qoobot.opencloud.common.domain.R;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleCreateDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleQueryDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleStatusDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.vo.AlertRuleVO;
import com.qoobot.opencloud.monitor.alert.rule.service.AlertRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警规则控制器
 */
@RestController
@RequestMapping("/api/monitor/alerts/rules")
@RequiredArgsConstructor
@Validated
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    /**
     * 分页查询告警规则
     */
    @GetMapping
    @PreAuthorize("hasAuthority('monitor:alert:rule:list')")
    public R<IPage<AlertRuleVO>> list(AlertRuleQueryDTO queryDTO) {
        return R.ok(alertRuleService.listRules(queryDTO));
    }

    /**
     * 获取告警规则详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:alert:rule:list')")
    public R<AlertRuleVO> getById(@PathVariable Long id) {
        return R.ok(alertRuleService.getRuleById(id));
    }

    /**
     * 创建告警规则
     */
    @PostMapping
    @PreAuthorize("hasAuthority('monitor:alert:rule:add')")
    public R<AlertRuleVO> create(@RequestBody @Validated AlertRuleCreateDTO createDTO) {
        return R.ok(alertRuleService.createRule(createDTO));
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:alert:rule:edit')")
    public R<AlertRuleVO> update(@PathVariable Long id,
                                  @RequestBody @Validated AlertRuleCreateDTO updateDTO) {
        return R.ok(alertRuleService.updateRule(id, updateDTO));
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:alert:rule:delete')")
    public R<Void> delete(@PathVariable Long id) {
        alertRuleService.deleteRule(id);
        return R.ok();
    }

    /**
     * 更新告警规则状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('monitor:alert:rule:edit')")
    public R<Void> updateStatus(@PathVariable Long id,
                                 @RequestBody @Validated AlertRuleStatusDTO statusDTO) {
        alertRuleService.updateRuleStatus(id, statusDTO);
        return R.ok();
    }
}
