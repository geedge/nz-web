package com.nis.modules.sys.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;

import com.nis.modules.sys.dao.SysI18nDao;
import com.nis.modules.sys.entity.SysI18nEntity;
import com.nis.modules.sys.service.SysI18nService;


@Service("sysI18nService")
public class SysI18nServiceImpl extends ServiceImpl<SysI18nDao, SysI18nEntity> implements SysI18nService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SysI18nEntity> page = this.page(
                new Query<SysI18nEntity>().getPage(params),
                new QueryWrapper<SysI18nEntity>()
                	.like(!StringUtils.isEmpty((String)params.get("code")), "code", (String)params.get("code"))
                	.eq(!StringUtils.isEmpty((String)params.get("lang")), "lang", (String)params.get("lang"))
                	.like(!StringUtils.isEmpty((String)params.get("value")), "value", (String)params.get("value"))
        );

        return new PageUtils(page);
    }

    public String queryValue(String code, String lang) {
    	QueryWrapper<SysI18nEntity> wrapper = new QueryWrapper<SysI18nEntity>();
    	wrapper.eq("code", code);
    	wrapper.eq("lang", lang);
    	List<SysI18nEntity> selectList = baseMapper.selectList(wrapper);
    	try {
			return selectList.get(0).getValue();
		} catch (Exception e) {
			return code;
		}
    }
    
	@Override
	public boolean check(SysI18nEntity sysI18n) {
		boolean dumpFlag = true; //记录是否重复(code+lang)，true为重复
		List<SysI18nEntity> list = list(new QueryWrapper<SysI18nEntity>()
									.eq(true, "code", sysI18n.getCode())
									. eq(true, "lang", sysI18n.getLang())
									. eq(true, "del_flag", Constant.DELFLAG_FALSE));
		//当只有一个结果并且这个结果是自身时，或没有结果时，说明不重复
		if ((list == null || list.size() == 0) ||
				(list != null && list.size() == 1 && list.get(0).getId() == sysI18n.getId())) {
			dumpFlag = false;
		}
		return dumpFlag;
	}
}