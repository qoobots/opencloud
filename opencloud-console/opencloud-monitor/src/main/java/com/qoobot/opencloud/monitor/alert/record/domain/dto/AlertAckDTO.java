package com.qoobot.opencloud.monitor.alert.record.domain.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 告警确认 DTO
 */
@Data
public class AlertAckDTO {

    @Size(max = 500, message = "确认备注长度不能超过500")
    private String ackNote;
}
