package com.qoobot.opencloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.auth.domain.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
