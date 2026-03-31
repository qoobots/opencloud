package com.qoobot.opencloud.system.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.system.log.domain.dto.OperationLogQueryDTO;
import com.qoobot.opencloud.system.log.domain.entity.SysOperationLog;
import com.qoobot.opencloud.system.log.domain.vo.OperationLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 操作日志 Mapper
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /**
     * 分页查询操作日志
     */
    IPage<OperationLogVO> selectPageVO(Page<OperationLogVO> page, @Param("q") OperationLogQueryDTO query, @Param("tenantId") String tenantId);

    /**
     * 导出操作日志（最大 10000 条）
     */
    List<OperationLogVO> selectForExport(@Param("q") OperationLogQueryDTO query, @Param("tenantId") String tenantId);
}
