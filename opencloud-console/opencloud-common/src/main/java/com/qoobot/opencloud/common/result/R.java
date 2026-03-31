package com.qoobot.opencloud.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应体
 *
 * @param <T> 数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "统一响应结果")
public class R<T> implements Serializable {

    @Schema(description = "状态码：200=成功，其他=失败")
    private Integer code;

    @Schema(description = "提示信息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳")
    private Long timestamp;

    private R() {
        this.timestamp = System.currentTimeMillis();
    }

    // ─── 成功 ──────────────────────────────────────────────────

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = ResultCode.SUCCESS.getMessage();
        r.data = data;
        return r;
    }

    public static <T> R<T> ok(String message, T data) {
        R<T> r = new R<>();
        r.code = ResultCode.SUCCESS.getCode();
        r.message = message;
        r.data = data;
        return r;
    }

    // ─── 失败 ──────────────────────────────────────────────────

    public static <T> R<T> fail() {
        return fail(ResultCode.INTERNAL_ERROR);
    }

    public static <T> R<T> fail(String message) {
        R<T> r = new R<>();
        r.code = ResultCode.INTERNAL_ERROR.getCode();
        r.message = message;
        return r;
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        R<T> r = new R<>();
        r.code = resultCode.getCode();
        r.message = resultCode.getMessage();
        return r;
    }

    public static <T> R<T> fail(Integer code, String message) {
        R<T> r = new R<>();
        r.code = code;
        r.message = message;
        return r;
    }

    // ─── 判断 ──────────────────────────────────────────────────

    public boolean isSuccess() {
        return ResultCode.SUCCESS.getCode().equals(this.code);
    }
}
