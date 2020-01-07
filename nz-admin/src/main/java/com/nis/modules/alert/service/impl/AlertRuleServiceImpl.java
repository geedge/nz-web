package com.nis.modules.alert.service.impl;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.module.entity.Module;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.project.entity.Project;
import com.nis.modules.project.service.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.alert.dao.AlertRuleDao;
import com.nis.modules.alert.entity.AlertNameMapUserDTO;
import com.nis.modules.alert.entity.AlertRuleEntity;
import com.nis.modules.alert.service.AlertRuleService;
import static com.nis.common.utils.Constant.AlertType;

@Service("alertRuleService")
public class AlertRuleServiceImpl extends ServiceImpl<AlertRuleDao, AlertRuleEntity> implements AlertRuleService {

	@Autowired
	private AssetService assetService;
	@Autowired
	private ModuleService moduleService;
	@Autowired
	private ProjectService projectService;
	@Autowired
	private AlertRuleDao alertRuleDao;
	
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
    	Integer searchId = null;
    	Integer searchType=null;
    	Integer searchLinkId=null;
    	if (params.get("id") != null && !StringUtils.isBlank(params.get("id").toString())) {
    		try{
				searchId = Integer.parseInt(params.get("id").toString());
			}catch (NumberFormatException e){}
    	}

    	if(params.get("type")!=null && StringUtils.isNotBlank(params.get("type").toString())){
			try{
				searchType = Integer.parseInt(params.get("type").toString());
			}catch (NumberFormatException e){}
		}
		if(params.get("linkId")!=null && StringUtils.isNotBlank(params.get("linkId").toString())){
			try{
				searchLinkId = Integer.parseInt(params.get("linkId").toString());
			}catch (NumberFormatException e){}
		}
    	IPage<AlertRuleEntity> page = this.page(
                new Query<AlertRuleEntity>().getPage(params),
                new QueryWrapper<AlertRuleEntity>()
                	.like(!StringUtils.isEmpty((String)params.get("alertName")), "alert_name", (String)params.get("alertName"))
                	.like(!StringUtils.isEmpty((String)params.get("expr")), "expr", (String)params.get("expr"))
                	.eq(!StringUtils.isEmpty((String)params.get("severity")), "severity", (String)params.get("severity"))
                	.eq(searchId != null, "id", searchId)
					.eq(params.get("type")!=null,"type",searchType)
					.eq(searchType!=null&&searchLinkId!=null,"link_id",searchLinkId)
        );

		List<AlertRuleEntity> records = page.getRecords();
		for (AlertRuleEntity alertRule:records){
			if(alertRule.getType()!=null){
				IService service=getServiceImpl(alertRule.getType());
				Object linkObject=service.getById(alertRule.getLinkId());
				if(AlertType.ALERT_TYPE_MODULE.getCode().intValue()==alertRule.getType().intValue()){
					Module module=(Module)linkObject;
					Project project = projectService.getById(module.getProjectId());
					module.setProject(project);
				}
				alertRule.setLinkObject(linkObject);
			}
		}

		return new PageUtils(page);
    }

	@Override
	public boolean checkName(AlertRuleEntity alertRule) {
		List<AlertRuleEntity> list = this.list(new QueryWrapper<AlertRuleEntity>()
				.eq("alert_name", alertRule.getAlertName()));
		
		//当只有一个结果并且这个结果是自身时，或没有结果时，说明名称不重复
		if ((list == null || list.size() == 0) ||
				(list.size() == 1 && alertRule.getId() != null && list.get(0).getId().intValue() == alertRule.getId().intValue())) {
			return true;
		}
		return false;
	}

	/**
	 * 校验linkId关联的对象是否存在
	 * */
	@Override
	public void validateTypeLink(Integer type,Integer linkId){
    	IService service=getServiceImpl(type);
		Object obj = service.getById(linkId);
		if(obj==null){
			throw new NZException(RCode.ALERTRULE_LINK_OBJECT_ISNULL);
		}
	}

	@Override
	public List<AlertNameMapUserDTO> queryUserByRuleId(Set<Integer> ruleIds) {
		List<AlertNameMapUserDTO> results=this.baseMapper.queryUserByRuleIds(ruleIds);
		return results;
	}

	/**
	 * 根据type获取相应service
	 * */
	private IService getServiceImpl(Integer typeId){
		Constant.AlertType alertType = Constant.AlertType.getAlertType(typeId);
		if(alertType==null){
			throw new NZException(RCode.ALERTRULE_TYPE_INVALIDE);
		}
		switch (alertType){
			case ALERT_TYPE_PROJECT:
				return projectService;
			case ALERT_TYPE_MODULE:
				return moduleService;
			case ALERT_TYPE_ASSET:
				return assetService;
		}
		return null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeByTypeIds(Integer typeId, List ids) {
		alertRuleDao.deleteByTypeIds(typeId,ids);
	}



}