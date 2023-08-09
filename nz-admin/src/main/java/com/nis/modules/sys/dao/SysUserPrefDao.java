package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysUserPreference;
import org.apache.ibatis.annotations.Mapper;

/**
 * System User Preference Dao
 *
 * @author shizhendong
 * @date 2023-08-09
 */
@Mapper
public interface SysUserPrefDao extends BaseMapper<SysUserPreference> {

}
