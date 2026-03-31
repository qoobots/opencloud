package com.qoobot.opencloud.monitor.alert.record.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * 告警记录详情 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlertRecordDetailVO extends AlertRecordVO {

    /**
     * AlertManager labels JSON
     */
    private Map<String, Object> labels;

    /**
     * AlertManager annotations JSON
     */
    private Map<String, Object> annotations;

    /**
     * 通知发送记录列表
     */
    private List<NotifyLogVO> notifyLogs;
}
