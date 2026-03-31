package com.qoobot.opencloud.system.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分页结果包装类
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private long pages;

    @Schema(description = "当前页")
    private long current;

    @Schema(description = "每页大小")
    private long size;

    @Schema(description = "数据列表")
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setCurrent(page.getCurrent());
        result.setSize(page.getSize());
        result.setRecords(page.getRecords());
        return result;
    }
}
