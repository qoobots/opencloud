package com.qoobot.opencloud.system.log.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 操作日志查询条件 DTO
 */
@Data
@Schema(description = "操作日志查询条件")
public class OperationLogQueryDTO {

    @Schema(description = "操作人用户名（模糊）")
    private String username;

    @Schema(description = "功能模块名（模糊）")
    private String moduleName;

    @Schema(description = "操作描述（模糊）")
    private String operationType;

    @Schema(description = "执行结果：SUCCESS / FAILED")
    private String status;

    @Schema(description = "开始时间")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime beginTime;

    @Schema(description = "结束时间")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;
}
