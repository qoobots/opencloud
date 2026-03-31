package com.qoobot.opencloud.system.log.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志表（由 @SysLog AOP 切面异步写入，只追加不修改）
 */
@Data
@TableName("sys_operation_log")
@Schema(description = "操作日志")
public class SysOperationLog implements Serializable {

    @Schema(description = "日志 ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "操作人用户 ID")
    private String userId;

    @Schema(description = "操作人所属租户 ID")
    private String tenantId;

    @Schema(description = "操作人用户名（冗余）")
    private String username;

    @Schema(description = "功能模块名（如「用户管理」）")
    private String moduleName;

    @Schema(description = "操作描述（如「新增用户」）")
    private String operationType;

    @Schema(description = "HTTP 方法")
    private String method;

    @Schema(description = "请求 URL")
    private String requestUrl;

    @Schema(description = "请求参数（JSON 格式，已脱敏）")
    private String requestParam;

    @Schema(description = "响应结果摘要（超长截断）")
    private String responseResult;

    @Schema(description = "异常信息（仅 status=FAILED 时有值）")
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
