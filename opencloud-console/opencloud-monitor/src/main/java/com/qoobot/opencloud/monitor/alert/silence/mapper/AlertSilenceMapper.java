package com.qoobot.opencloud.monitor.alert.silence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.monitor.alert.silence.domain.entity.MonitorAlertSilence;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警静默 Mapper
 */
@Mapper
public interface AlertSilenceMapper extends BaseMapper<MonitorAlertSilence> {

    /**
     * 查询当前租户下生效中的静默规则（status=ACTIVE 且时间段覆盖当前时刻）
     */
    @Select("SELECT * FROM monitor_alert_silence " +
            "WHERE tenant_id = #{tenantId} " +
            "AND status = 'ACTIVE' " +
            "AND start_at <= #{now} " +
            "AND end_at >= #{now}")
    List<MonitorAlertSilence> findActiveSilences(@Param("tenantId") String tenantId,
                                                  @Param("now") LocalDateTime now);

    /**
     * 将已过期（end_at < now）的 ACTIVE 静默批量更新为 EXPIRED
     */
    @Select("UPDATE monitor_alert_silence SET status = 'EXPIRED', updated_at = NOW() " +
            "WHERE status = 'ACTIVE' AND end_at < #{now}")
    void expireOutdated(@Param("now") LocalDateTime now);
}
