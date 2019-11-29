package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.asset.entity.Asset;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 资产    dao
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Mapper
@Repository
public interface AssetDao extends BaseMapper<Asset> {

    List<Asset> queryList(IPage<Asset> page, Map<String, Object> params);

	List<Asset> queryListByHosts(Collection<String> hosts);
}