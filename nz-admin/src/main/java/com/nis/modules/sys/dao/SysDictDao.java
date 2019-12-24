/**

 *

 *

 */

package com.nis.modules.sys.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.sys.entity.SysDictEntity;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 数据字典
 *

 */
@Mapper
public interface SysDictDao extends BaseMapper<SysDictEntity> {
	List<SysDictEntity> queryNodeImgList();

    List<SysDictEntity> querySysDictListWithPage(IPage<SysDictEntity> page, @Param("params")Map<String, Object> params);
    List<SysDictEntity> querySysDictList(@Param("params")Map<String, Object> params);

    Integer getMaxTypeCode(@Param("type")String type);
}
