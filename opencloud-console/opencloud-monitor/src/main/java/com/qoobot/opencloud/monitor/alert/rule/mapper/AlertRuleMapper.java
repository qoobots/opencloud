package com.qoobot.opencloud.monitor.alert.rule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.monitor.alert.rule.domain.entity.MonitorAlertRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 告警规则 Mapper
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<MonitorAlertRule> {

    /**
     * 分页查询告警规则
     */
    IPage<MonitorAlertRule> selectPageVO(Page<MonitorAlertRule> page,
                                          @Param("tenantId") String tenantId,
                                          @Param("q") Object query);

    /**
     * 按规则名和租户查询
     */
    @Select("SELECT * FROM monitor_alert_rule WHERE tenant_id = #{tenantId} " +
            "AND rule_name = #{ruleName} AND deleted = 0 LIMIT 1")
    MonitorAlertRule findByRuleName(@Param("ruleName") String ruleName,
                                     @Param("tenantId") String tenantId);

    /**
     * 按 alertName 匹配规则
     */
    @Select("SELECT * FROM monitor_alert_rule WHERE tenant_id = #{tenantId} " +
            "AND rule_name = #{alertName} AND status = 'ENABLED' AND deleted = 0 LIMIT 1")
    MonitorAlertRule findByAlertName(@Param("alertName") String alertName,
                                      @Param("tenantId") String tenantId);

    /**
     * 检查通知渠道是否被引用
     */
    @Select("SELECT COUNT(*) FROM monitor_alert_rule " +
            "WHERE notify_channel_ids LIKE CONCAT('%', #{channelId}, '%') " +
            "AND deleted = 0")
    int countByChannelId(@Param("channelId") Long channelId);
}
