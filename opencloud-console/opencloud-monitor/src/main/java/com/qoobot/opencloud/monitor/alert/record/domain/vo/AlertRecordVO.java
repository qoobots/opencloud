package com.qoobot.opencloud.monitor.alert.record.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警记录 VO
 */
@Data
public class AlertRecordVO {

    private Long id;

    private Long ruleId;

    private String ruleName;

    private String alertName;

    private String severity;

    private String instance;

    private String summary;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;

    private String ackBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime ackAt;

    private String ackNote;

    private Boolean notified;
}
