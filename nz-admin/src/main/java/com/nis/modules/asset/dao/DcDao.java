package com.nis.modules.asset.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nis.modules.asset.entity.Dc;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 数据中心    dao
 *
 * @author shizhendong
 * @date 2021-4-6
 */
@Mapper
@Repository
public interface DcDao extends BaseMapper<Dc> {

}
