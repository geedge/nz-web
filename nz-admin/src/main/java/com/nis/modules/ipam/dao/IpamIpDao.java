package com.nis.modules.ipam.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nis.modules.ipam.entity.IpamIp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface IpamIpDao extends BaseMapper<IpamIp> {

    List<IpamIp> queryPage(IPage page, @Param("params") Map params);
}
