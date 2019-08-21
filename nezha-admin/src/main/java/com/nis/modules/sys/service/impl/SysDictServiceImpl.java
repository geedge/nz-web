/**

 *

 *

 */

package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Query;
import com.nis.modules.sys.dao.SysDictDao;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;


@Service("sysDictService")
public class SysDictServiceImpl extends ServiceImpl<SysDictDao, SysDictEntity> implements SysDictService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        String name = (String)params.get("name");
        String type = (String)params.get("type");

        IPage<SysDictEntity> page = this.page(
            new Query<SysDictEntity>().getPage(params),
            new QueryWrapper<SysDictEntity>()
                .like(StringUtils.isNotBlank(name),"name", name)
                .eq(StringUtils.isNotBlank(type), "type", type)
                .eq("del_flag", Constant.DELFLAG_FALSE)
        );

        return new PageUtils(page);
    }

	@Override
	public List<SysDictEntity> queryList(Map<String, Object> params) {
		String name = (String)params.get("name");
        String type = (String)params.get("type");
        
        QueryWrapper<SysDictEntity> wrapper = new QueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(name), "name", name)
        		.eq(StringUtils.isNotBlank(type), "type", type)
        		.eq("del_flag", Constant.DELFLAG_FALSE);
        List<SysDictEntity> list = this.list(wrapper);
        
		return list;
	}
	
	@Override
	public boolean delete(List<Long> idList) {
		UpdateWrapper<SysDictEntity> wrapper = new UpdateWrapper<>();
		wrapper.in("id", idList);
		wrapper.eq("del_flag", Constant.DELFLAG_FALSE);
		wrapper.set("del_flag", Constant.DELFLAG_TRUE);
		wrapper.set("operator", ShiroUtils.getUserId());
		wrapper.set("op_time", new Date());
		return update(wrapper);
	}

}
