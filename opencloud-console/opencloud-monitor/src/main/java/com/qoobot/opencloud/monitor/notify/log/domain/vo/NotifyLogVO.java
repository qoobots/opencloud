package com.qoobot.opencloud.monitor.notify.log.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知日志 VO
 */
@Data
public class NotifyLogVO {

    private Long id;

    private Long alertRecordId;

    private Long channelId;

    private String channelName;

    private String channelType;

    private Boolean isTest;

    private String status;

    private String errorMsg;

    private Integer retryCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;
}
