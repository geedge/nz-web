package com.nis.modules.ipam.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.ipam.entity.IpamSubnet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Mapper
@Repository
public interface IpamSubnetDao extends BaseMapper<IpamSubnet> {
	
	List<IpamSubnet> queryPage(IPage page, @Param("params")Map<String, Object> params);
	
}
