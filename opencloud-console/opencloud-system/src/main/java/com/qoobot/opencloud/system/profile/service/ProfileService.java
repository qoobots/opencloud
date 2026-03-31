package com.qoobot.opencloud.system.profile.service;

import com.qoobot.opencloud.system.profile.domain.dto.ProfileUpdateDTO;
import com.qoobot.opencloud.system.profile.domain.vo.ProfileVO;

/**
 * 个人中心 Service
 */
public interface ProfileService {

    /** 获取当前登录用户个人信息 */
    ProfileVO getProfile();

    /** 修改个人信息 */
    void updateProfile(ProfileUpdateDTO dto);
}
