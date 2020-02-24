package com.nis.modules.sys.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.sys.dao.SysAreaDao;
import com.nis.modules.sys.entity.SysArea;
import com.nis.modules.sys.service.SysAreaService;

import io.micrometer.core.instrument.util.StringUtils;

@Service("sysAreaService")
public class SysAreaServiceImpl extends ServiceImpl<SysAreaDao, SysArea> implements SysAreaService {

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		String id = (String) params.get("id");
		String pid = (String)params.get("pid");
		String name = (String) params.get("name");
		String includeLeaf = (String)params.get("includeLeaf");
		
		IPage<SysArea> page = this.page(
				new Query<SysArea>().getPage(params),
				new QueryWrapper<SysArea>().eq(StringUtils.isNotBlank(id),"id",id)
				.like(StringUtils.isNotBlank(name),"name",name)
				.eq(StringUtils.isNotBlank(pid), "pid",pid)
			    );
		
		// 查询子集列表数据
		if(StringUtils.isBlank(includeLeaf)) {
			includeLeaf="1";
		}
		if(!includeLeaf.equals("0")) {
			Map<Long,SysArea> map =new HashMap<Long,SysArea>(); 
			List<SysArea> datas = page.getRecords();
			List<SysArea> allDatas = this.list();
			for(SysArea data: allDatas) {
				map.put(data.getId().longValue(), data);
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
		
		return new PageUtils(page);
	}

}
