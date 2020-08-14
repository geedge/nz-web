/**

 *

 *

 */

package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.utils.CommonUtil;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Model;
import com.nis.modules.asset.service.ModelService;
import com.nis.modules.sys.dao.SysDictDao;
import com.nis.modules.sys.entity.SysDictEntity;
import com.nis.modules.sys.service.SysDictService;
import com.nis.modules.sys.service.SysI18nService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service("sysDictService")
public class SysDictServiceImpl extends ServiceImpl<SysDictDao, SysDictEntity> implements SysDictService {

	@Autowired
	private SysI18nService i18nService;
	@Autowired
	private ModelService modelService;
	
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
		IPage<SysDictEntity> page=this.page(new Query<SysDictEntity>().getPage(params));

		List<SysDictEntity> list=this.baseMapper.querySysDictListWithPage(page,params);

		page.setRecords(list);
        return new PageUtils(page);
    }

	@Override
	public List<SysDictEntity> queryList(Map<String, Object> params) {
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
	public void delete(String ids) {
		if(ToolUtil.isEmpty(ids)) {
			return;
		}
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

    	//当字典表中数据为assetType或vendor类型时  有关联model不允许删除
    	validateModel(delIds);
    	
    	this.removeByIds(delIds);
    	
	}
	
	// 校验参数逻辑 model相关
	public void validateModel(List<Integer> ids) {
		List<SysDictEntity> result = this.list(new QueryWrapper<SysDictEntity>().in("id", ids));
    	SysDictEntity sysDictEntity = result.get(0);
    	String param="";
    	if(sysDictEntity.getType().equals(Constant.DICT_ASSET_MODEL_TYPE)) {
    		param="type";
    	}else if(sysDictEntity.getType().equals(Constant.DICT_ASSET_MODEL_VENDOR)) {
    		param=Constant.DICT_ASSET_MODEL_VENDOR;
    	}
    	if(ToolUtil.isNotEmpty(param)) {
    		Set<Integer> modelParamIds = result.stream().map(SysDictEntity::getCode).collect(Collectors.toSet());
    		List<Model> list = modelService.list(new QueryWrapper<Model>().in(param, modelParamIds));
    		if(ToolUtil.isNotEmpty(list)) {
    			throw new NZException(RCode.SYS_DICT_REMOVE_ERROR);
    		}
    	}
    	
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
    				new QueryWrapper<SysDictEntity>().lambda()
					.eq(SysDictEntity::getType,dict.getType())
					.eq(SysDictEntity::getCode,dict.getCode())
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
		List<SysDictEntity> list = this.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getName, name).eq(SysDictEntity::getType, type).eq(SysDictEntity::getValue, value));

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
		List<SysDictEntity> list = this.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, type).eq(SysDictEntity::getValue, value).eq(SysDictEntity::getDelFlag, 0));
		if(list==null ||list.size()<1){
			throw new NZException(errCode);
		}
	}
	@Override
	public void checkSysDicCode(String type, Integer code, RCode errCode) {
		List<SysDictEntity> list = this.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, type).eq(SysDictEntity::getCode, code).eq(SysDictEntity::getDelFlag, 0));
		if(list==null ||list.size()<1){
			throw new NZException(errCode);
		}
	}

	@Override
	public List<SysDictEntity> querySysDicByType(String type) {
		List<SysDictEntity> list = this.list(new QueryWrapper<SysDictEntity>().lambda().eq(SysDictEntity::getType, type).eq(SysDictEntity::getDelFlag, 0));
		return list;
	}

	@Override
	public void saveSysDict(SysDictEntity dict) {
		//字典名称，非必填项，为空后台自动添加value值
		if(StringUtils.isBlank(dict.getName())){
			dict.setName(dict.getValue());
		}
		//字典码，当字典类型为：vendor,assetType时，可以不填，后台根据已有最大code值 +1 填写
		if( dict.getCode()==null && Constant.SPECIAL_SYS_DICT.contains(dict.getType())){
			Integer maxCode=this.queryMaxTypeCode(dict.getType());
			if(maxCode != null){
				maxCode+=1;
			}else{
				maxCode=1;
			}

			dict.setCode(maxCode);
		}else{
			throw new NZException(RCode.SYS_DICT_CODE_ISNULL);
		}

		// 如果是assetType 或者 vendor 类型校验名称重复
		if(Constant.DICT_ASSET_MODEL_TYPE.equals(dict.getType()) || Constant.DICT_ASSET_MODEL_VENDOR.equals(dict.getType())){
			this.validateAssetTypeVendorValue(dict);
		}

		Long userId = ShiroUtils.getUserId();
		dict.setOperator(userId);
		dict.setOpTime(DateUtil.getUTCTimeByConfigTimeZone());
		this.save(dict);
	}

	/**
	 * assetType vendor 类型校验 value值是否重复
	 * @param dict
	 */
	private void validateAssetTypeVendorValue(SysDictEntity dict) {
		List<SysDictEntity> list = this.list(new QueryWrapper<SysDictEntity>().lambda().in(SysDictEntity::getType, Constant.DICT_ASSET_MODEL_TYPE, Constant.DICT_ASSET_MODEL_VENDOR).eq(SysDictEntity::getDelFlag,0));

		List<SysDictEntity> assetTypeDictList = new ArrayList<>();
		List<SysDictEntity> vendorDictList = new ArrayList<>();

		for (SysDictEntity entity : list) {
			switch (entity.getType()) {
				case Constant.DICT_ASSET_MODEL_TYPE: {
					assetTypeDictList.add(entity);
					break;
				}
				case Constant.DICT_ASSET_MODEL_VENDOR: {
					vendorDictList.add(entity);
					break;
				}
				default:
			}
		}

		switch (dict.getType()) {
			case Constant.DICT_ASSET_MODEL_TYPE: {
				List<String> assetTypes = assetTypeDictList.stream().map(SysDictEntity::getValue).collect(Collectors.toList());
				if (assetTypes.contains(dict.getValue())) {
					throw new NZException(RCode.ASSET_TYPE_VALUE_DUPLICATE);
				}
				break;
			}
			case Constant.DICT_ASSET_MODEL_VENDOR: {
				List<String> vendors = vendorDictList.stream().map(SysDictEntity::getValue).collect(Collectors.toList());
				if (vendors.contains(dict.getValue())) {
					throw new NZException(RCode.ASSET_VENDOR_VALUE_DUPLICATE);
				}
				break;
			}
			default:
		}
	}
}
