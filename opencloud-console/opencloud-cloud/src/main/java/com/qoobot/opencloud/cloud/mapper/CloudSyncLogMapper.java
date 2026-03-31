package com.qoobot.opencloud.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.cloud.domain.entity.CloudSyncLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资源同步日志 Mapper
 */
@Mapper
public interface CloudSyncLogMapper extends BaseMapper<CloudSyncLog> {
}
