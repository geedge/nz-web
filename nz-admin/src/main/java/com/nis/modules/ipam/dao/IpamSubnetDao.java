package com.nis.modules.ipam.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.ipam.entity.IpamSubnet;

@Mapper
@Repository
public interface IpamSubnetDao extends BaseMapper<IpamSubnet> {
	
	List<IpamSubnet> queryPage(IPage page, @Param("params")Map<String, Object> params);
	
}
