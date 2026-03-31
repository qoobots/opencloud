package com.qoobot.opencloud.monitor.alert.record.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertAckDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertRecordQueryDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertStatsQueryDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertRecordDetailVO;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertRecordVO;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertStatsVO;

/**
 * 告警记录服务接口
 */
public interface AlertRecordService {

    /**
     * 分页查询告警记录
     */
    IPage<AlertRecordVO> listRecords(AlertRecordQueryDTO queryDTO);

    /**
     * 获取告警记录详情
     */
    AlertRecordDetailVO getRecordById(Long id);

    /**
     * 确认告警
     */
    void ackRecord(Long id, AlertAckDTO ackDTO);

    /**
     * 获取告警统计
     */
    AlertStatsVO getStats(AlertStatsQueryDTO queryDTO);
}
