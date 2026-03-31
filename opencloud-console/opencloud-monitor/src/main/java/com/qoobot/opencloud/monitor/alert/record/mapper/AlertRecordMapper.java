package com.qoobot.opencloud.monitor.alert.record.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.monitor.alert.record.domain.entity.MonitorAlertRecord;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 告警记录 Mapper
 */
@Mapper
public interface AlertRecordMapper extends BaseMapper<MonitorAlertRecord> {

    /**
     * 分页查询告警记录
     */
    IPage<MonitorAlertRecord> selectPageVO(Page<MonitorAlertRecord> page,
                                            @Param("tenantId") String tenantId,
                                            @Param("q") Object query);

    /**
     * 按去重键查询（alertName + instance + firedAt）
     */
    @Select("SELECT * FROM monitor_alert_record " +
            "WHERE alert_name = #{alertName} AND instance = #{instance} AND fired_at = #{firedAt} " +
            "LIMIT 1")
    MonitorAlertRecord findByDedup(@Param("alertName") String alertName,
                                    @Param("instance") String instance,
                                    @Param("firedAt") LocalDateTime firedAt);

    /**
     * 查询 FIRING 状态的记录（用于 resolved 时更新）
     */
    @Select("SELECT * FROM monitor_alert_record " +
            "WHERE alert_name = #{alertName} AND instance = #{instance} " +
            "AND status IN ('FIRING', 'ACKNOWLEDGED') " +
            "ORDER BY fired_at DESC LIMIT 1")
    MonitorAlertRecord findFiringByAlertAndInstance(@Param("alertName") String alertName,
                                                     @Param("instance") String instance);

    /**
     * 统计告警数据
     */
    AlertStatsVO selectStats(@Param("tenantId") String tenantId,
                             @Param("beginTime") LocalDateTime beginTime);
}
