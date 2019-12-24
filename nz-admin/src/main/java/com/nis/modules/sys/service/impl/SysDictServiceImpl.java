/**

 *

 *

 */

package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.modules.sys.dao.SysDictDao;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.SysI18nService;
import com.nis.modules.sys.shiro.ShiroUtils;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service("sysDictService")
public class SysDictServiceImpl extends ServiceImpl<SysDictDao, SysDictEntity> implements SysDictService {

	@Autowired
	private SysI18nService i18nService;
	
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
//        String name = (String)params.get("value");
//        String type = (String)params.get("type");

//        IPage<SysDictEntity> page = this.page(
//            new Query<SysDictEntity>().getPage(params),
//            new QueryWrapper<SysDictEntity>()
//                .like(StringUtils.isNotBlank(name),"value", name)
//                .eq(StringUtils.isNotBlank(type), "type", type)
//                .eq("del_flag", Constant.DELFLAG_FALSE)
//        );
		IPage<SysDictEntity> page=this.page(new Query<SysDictEntity>().getPage(params));

		List<SysDictEntity> list=this.baseMapper.querySysDictListWithPage(page,params);

		page.setRecords(list);

        return new PageUtils(page);
    }

	@Override
	public List<SysDictEntity> queryList(Map<String, Object> params) {
//		String name = (String)params.get("name");
//        String type = (String)params.get("type");
        
//        QueryWrapper<SysDictEntity> wrapper = new QueryWrapper<>();
//        wrapper.eq(StringUtils.isNotBlank(name), "name", name)
//        		.eq(StringUtils.isNotBlank(type), "type", type)
//        		.eq("del_flag", Constant.DELFLAG_FALSE);
//        List<SysDictEntity> list = this.list(wrapper);
		List<SysDictEntity> list = this.baseMapper.querySysDictList(params);
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
	public boolean delete(String ids) {

    	String[] idArr=ids.split(",");
    	ArrayList<Integer> delIds=new ArrayList<Integer>();
    	for(String idStr:idArr){
    		try {
    			Integer id = Integer.parseInt(idStr);
    			delIds.add(id);
			}catch (NumberFormatException e){
    			throw new NZException(RCode.SYS_DICT_ID_INVALIDE);
			}
		}

		UpdateWrapper<SysDictEntity> wrapper = new UpdateWrapper<>();
		wrapper.in("id", delIds);
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
		SysDictEntity old =null;
    	if(dict.getId()!=null){
    		old = getById(dict.getId());
		}else{
    		old = this.baseMapper.selectOne(
    				new QueryWrapper<SysDictEntity>()
					.eq("type",dict.getType())
					.eq("code",dict.getCode())
				  );
		}
		if(old == null){
			throw new NZException(RCode.SYS_ERROR);
		}

		dict.setId(old.getId());

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

	@Override
	public Integer queryMaxTypeCode(String type) {
		Integer maxCode=this.baseMapper.getMaxTypeCode(type);
		return maxCode;
	}

	@Override
	public void checkSysDicValue(String type, String value, RCode errCode) {
		QueryWrapper<SysDictEntity> queryWrapper=new QueryWrapper<SysDictEntity>();
		queryWrapper.eq("type",type);
		queryWrapper.eq("value",value);
		queryWrapper.eq("del_flag",0);
		List<SysDictEntity> list = this.list(queryWrapper);
		if(list==null ||list.size()<1){
			throw new NZException(errCode);
		}
	}
	@Override
	public void checkSysDicCode(String type, Integer code, RCode errCode) {
		QueryWrapper<SysDictEntity> queryWrapper=new QueryWrapper<SysDictEntity>();
		queryWrapper.eq("type",type);
		queryWrapper.eq("code",code);
		queryWrapper.eq("del_flag",0);
		List<SysDictEntity> list = this.list(queryWrapper);
		if(list==null ||list.size()<1){
			throw new NZException(errCode);
		}
	}

	@Override
	public List<SysDictEntity> querySysDicByType(String type) {
		QueryWrapper<SysDictEntity> queryWrapper=new QueryWrapper<SysDictEntity>();
		queryWrapper.eq("type",type);
		queryWrapper.eq("del_flag",0);
		List<SysDictEntity> list = this.list(queryWrapper);
		return list;
	}


}
