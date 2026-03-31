package com.qoobot.opencloud.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.auth.domain.entity.UserCredential;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户凭证 Mapper
 */
@Mapper
public interface UserCredentialMapper extends BaseMapper<UserCredential> {
}
