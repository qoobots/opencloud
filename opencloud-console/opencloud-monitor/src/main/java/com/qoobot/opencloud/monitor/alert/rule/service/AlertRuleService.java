package com.qoobot.opencloud.monitor.alert.rule.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleCreateDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleQueryDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleStatusDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.vo.AlertRuleVO;

/**
 * 告警规则服务接口
 */
public interface AlertRuleService {

    /**
     * 分页查询告警规则
     */
    IPage<AlertRuleVO> listRules(AlertRuleQueryDTO queryDTO);

    /**
     * 获取告警规则详情
     */
    AlertRuleVO getRuleById(Long id);

    /**
     * 创建告警规则
     */
    AlertRuleVO createRule(AlertRuleCreateDTO createDTO);

    /**
     * 更新告警规则
     */
    AlertRuleVO updateRule(Long id, AlertRuleCreateDTO updateDTO);

    /**
     * 删除告警规则
     */
    void deleteRule(Long id);

    /**
     * 更新告警规则状态
     */
    void updateRuleStatus(Long id, AlertRuleStatusDTO statusDTO);
}
