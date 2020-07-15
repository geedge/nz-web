/**

 *

 *

 */

package com.nis.modules.sys.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SysLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 系统日志
 *

 */
@Mapper
public interface SysLogDao extends BaseMapper<SysLogEntity> {

    List<SysLogEntity> queryList(IPage<SysLogEntity> page, @Param("params") Map<String, Object> params);
}
