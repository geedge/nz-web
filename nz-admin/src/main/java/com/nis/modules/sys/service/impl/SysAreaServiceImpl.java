package com.nis.modules.sys.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.additional.query.impl.QueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.endpoint.entity.MetadataMetric;
import com.nis.modules.sys.dao.SysAreaDao;
import com.nis.modules.sys.entity.SysArea;
import com.nis.modules.sys.service.SysAreaService;

import io.micrometer.core.instrument.util.StringUtils;

@Service("sysAreaService")
public class SysAreaServiceImpl extends ServiceImpl<SysAreaDao, SysArea> implements SysAreaService {
	private static Logger logger = LoggerFactory.getLogger(SysAreaServiceImpl.class);
	@Autowired
	private RedisTemplate<String,String> redisTemplate;
	
	@SuppressWarnings("unchecked")
	@Override
	public Map queryPage(Map<String, Object> params) {
		Map result=new HashMap();
		String id = (String) params.get("id");
		String pid = (String)params.get("pid");
		String name = (String) params.get("name");
		String includeLeaf = (String)params.get("includeLeaf");
		
		List<SysArea> datas = this.list(new QueryWrapper<SysArea>().eq(StringUtils.isNotBlank(id),"id",id)
				.like(StringUtils.isNotBlank(name),"name",name)
				.eq(StringUtils.isNotBlank(pid), "pid",pid));
		// 查询子集列表数据
		if(StringUtils.isBlank(includeLeaf)) {
			includeLeaf="1";
		}
		if(!includeLeaf.equals("0")) {
			Map<Long,SysArea> map =new HashMap<Long,SysArea>(); 
			Object areaCache = redisTemplate.opsForValue().get(Constant.REDIS_KEY_AREA);
			List<SysArea> allDatas = JSONObject.parseArray(String.valueOf(areaCache), SysArea.class);
			for(SysArea data: allDatas) {
				map.put(data.getId(), data);
			}
			// 父子类关系
			for(SysArea data: allDatas) {
				SysArea parent = map.get(data.getPid().longValue());
				if(parent!=null) {
					List<SysArea> children = parent.getChildren();
					if(children==null) {
						children=new ArrayList<SysArea>();
					}
					children.add(data);
					parent.setChildren(children);
				}
			}
			for(SysArea data: datas) {
				long i = data.getId().longValue();
				SysArea sysArea = (SysArea) map.get(i);
				if(sysArea!=null) {
					data.setChildren(sysArea.getChildren());
				}
			}
		}
		if(ObjectUtils.isEmpty(datas)) {
			datas=new ArrayList();
		}
		result.put("list", datas);
		return result;
	}

}
