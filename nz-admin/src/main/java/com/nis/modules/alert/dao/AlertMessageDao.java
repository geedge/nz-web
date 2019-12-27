package com.nis.modules.alert.dao;

import com.nis.modules.alert.entity.AlertMessageEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 
 * 
 * @author song
 * @email 
 * @date 2019-11-25 10:11:01
 */
@Mapper
public interface AlertMessageDao extends BaseMapper<AlertMessageEntity> {

	IPage<AlertMessageEntity> list(IPage<AlertMessageEntity> page, @Param("am") AlertMessageEntity object);

	void updateBatch(@Param("hashKeys") List<String> updateCacheKeys);
}
