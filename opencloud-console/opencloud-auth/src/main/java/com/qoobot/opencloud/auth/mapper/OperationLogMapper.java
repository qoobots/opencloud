package com.qoobot.opencloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.auth.domain.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作审计日志 Mapper
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
