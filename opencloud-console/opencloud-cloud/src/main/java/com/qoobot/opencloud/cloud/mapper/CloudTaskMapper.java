package com.qoobot.opencloud.cloud.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.cloud.domain.entity.CloudTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 异步任务 Mapper
 */
@Mapper
public interface CloudTaskMapper extends BaseMapper<CloudTask> {

    @Select("SELECT * FROM cloud.cloud_task WHERE task_id = #{taskId}")
    CloudTask selectByTaskId(@Param("taskId") String taskId);
}
