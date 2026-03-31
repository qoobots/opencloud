package com.qoobot.opencloud.system.log.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志 VO
 */
@Data
@Schema(description = "操作日志")
public class OperationLogVO {

    @Schema(description = "日志 ID")
    private Long id;

    @Schema(description = "操作人用户 ID")
    private String userId;

    @Schema(description = "操作人用户名")
    private String username;

    @Schema(description = "功能模块名")
    private String moduleName;

    @Schema(description = "操作描述")
    private String operationType;

    @Schema(description = "HTTP 方法")
    private String method;

    @Schema(description = "请求 URL")
    private String requestUrl;

    @Schema(description = "请求参数")
    private String requestParam;

    @Schema(description = "响应结果摘要")
    private String responseResult;

    @Schema(description = "异常信息")
    private String exceptionMsg;

    @Schema(description = "执行结果：SUCCESS / FAILED")
    private String status;

    @Schema(description = "接口耗时（毫秒）")
    private Integer costTime;

    @Schema(description = "操作人 IP")
    private String operateIp;

    @Schema(description = "操作时间")
    private LocalDateTime operateTime;
}
