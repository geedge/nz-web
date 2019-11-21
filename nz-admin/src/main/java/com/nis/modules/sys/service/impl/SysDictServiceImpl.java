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
import com.nis.modules.sys.service.SysI18nService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service("sysDictService")
public class SysDictServiceImpl extends ServiceImpl<SysDictDao, SysDictEntity> implements SysDictService {

	@Autowired
	private SysI18nService i18nService;
	
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
        if (!CommonUtil.isNull(list)) {
        	for (SysDictEntity s : list) {
        		if (!CommonUtil.isNull(s.getI18nCode())) {
        			String queryValue = i18nService.queryValue(s.getI18nCode());
        			s.setValue(queryValue);
        		}
        	}
        }
        
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
	
	@Override
	public boolean save(SysDictEntity dict) {
		super.save(dict);
		if (dict.getType().equals(Constant.DICT_NAME_LANG)) {
			updateLangCache();
		}
		return true;
	}
	
	@Override
	public boolean update(SysDictEntity dict) {
		SysDictEntity old = getById(dict.getId());
		boolean langUpdate = false;
		//若更新了语言，刷新语言缓存
		if (dict.getType().equals(Constant.DICT_NAME_LANG) && !old.getValue().equals(dict.getValue())) {
			langUpdate = true;
		}
		updateById(dict);
		if (langUpdate) {
			updateLangCache();
		}
		return true;
	}

	public void updateLangCache() {
		Constant.langList.clear();
		HashMap<String, Object> params = new HashMap<>();
		params.put("type", Constant.DICT_NAME_LANG);
		List<SysDictEntity> queryList = queryList(params);
		for (SysDictEntity d : queryList) {
			Map<String, String> lang = new HashMap<>();
			lang.put("name", d.getRemark());
			lang.put("value", d.getValue());
			Constant.langList.add(lang);
		}
	}


	/**
	 *  name type 下 value 值不能重复
	 * @param id
	 * @param name   字典名称
	 * @param type   字典类型
	 * @param value  字典值
	 * @return
	 */
	@Override
	public boolean dictValueCheck(Long id, String name, String type, String value) {
		List<SysDictEntity> list = this.list(new QueryWrapper<SysDictEntity>().eq("name", name).eq("type", type).eq("value", value));

		boolean flag = false;
		if (list == null || list.size() == 0 || (list != null && list.size() == 1 && list.get(0).getId().equals(id))) {
			flag = true;
		}
		return flag;
	}

}
