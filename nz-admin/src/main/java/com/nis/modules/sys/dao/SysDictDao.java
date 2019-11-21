/**

 *

 *

 */

package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.sys.entity.SysDictEntity;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

/**
 * 数据字典
 *

 */
@Mapper
public interface SysDictDao extends BaseMapper<SysDictEntity> {
	List<SysDictEntity> queryNodeImgList();
	
}
