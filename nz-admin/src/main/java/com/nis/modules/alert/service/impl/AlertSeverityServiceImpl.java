package com.nis.modules.alert.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertSeverityDao;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.alert.service.AlertNotifyMethodService;
import com.nis.modules.alert.service.AlertSeverityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("alertSeverityService")
public class AlertSeverityServiceImpl extends ServiceImpl<AlertSeverityDao, AlertSeverityConf> implements AlertSeverityService {
    private static final Log logger= Log.get();

    @Autowired
    private AlertSeverityDao alertSeverityDao;

    @Autowired
    private AlertNotifyMethodService alertNotifyMethodService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage page = new Query(AlertSeverityConf.class).getPage(params);

        List<AlertSeverityConf> list = alertSeverityDao.queryList(page,params);
        page.setRecords(list);

        return new PageUtils(page);
    }

    @Override
    public List<AlertSeverityConf> queryList(Map<String, Object> params) {

        List<AlertSeverityConf> list = this.list(new QueryWrapper<AlertSeverityConf>().lambda()
                                                    .in(ToolUtil.isNotEmpty(params.get("ids")),AlertSeverityConf::getId,String.valueOf(params.get("ids")).split(","))
                                                    .like(ToolUtil.isNotEmpty(params.get("name")),AlertSeverityConf::getName,Tool.StrUtil.likeEscape(Tool.MapUtil.getStr(params,"name")))
                                                );

        return list;
    }

    @Override
    public void validateSeverity(AlertSeverityConf alertSeverityConf, boolean isModify) {
        ValidateUtils validateUtils = ValidateUtils.is(alertSeverityConf.getName()).notNull(RCode.ALERTSEVERITY_NAME_ISNULL)
                .and(alertSeverityConf.getColor()).notNull(RCode.ALERTSEVERITY_COLOR_ISNULL)
                .and(alertSeverityConf.getRemark()).notNull(RCode.ALERTSEVERITY_REMARK_ISNULL);
        if(isModify){
            validateUtils.and(alertSeverityConf.getId()).notNull(RCode.ALERTSEVERITY_ID_ISNULL);
        }
    }

    @Override
    public void setWeight(AlertSeverityConf alertSeverityConf) {
        AlertSeverityConf max = this.getOne(new QueryWrapper<AlertSeverityConf>().select("id","weight").orderByDesc("weight").last("limit 1"));
        if(ToolUtil.isEmpty(max)){
            alertSeverityConf.setWeight(0);
        }else{
            alertSeverityConf.setWeight(max.getWeight()+1);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWeights(List<AlertSeverityConf> alertSeverityConfs) {
        //校验传递的id weight是否为空
        AlertSeverityConf alertSeverityConf = alertSeverityConfs.stream().filter(item -> ToolUtil.isEmpty(item.getId()) || ToolUtil.isEmpty(item.getWeight())).findAny().orElse(null);
        if(alertSeverityConf != null)
            throw new NZException(RCode.ALERTSEVERITY_WEIGHT_PARAM_ISNULL);

        alertSeverityDao.saveWeightBatch(alertSeverityConfs);
    }

    @Override
    public AlertSeverityConf getSeverityById(Integer id){
        AlertSeverityConf alertSeverityConf = super.getById(id);
        return alertSeverityConf;
    }

    @Override
    public void saveSeverity(AlertSeverityConf alertSeverityConf) {
        this.setWeight(alertSeverityConf);
        this.save(alertSeverityConf);
    }

    @Override
    public void updateSeverity(AlertSeverityConf alertSeverityConf) {
        this.updateById(alertSeverityConf);
    }

	@Override
	public Map<Integer, AlertSeverityConf> queryMap(Map<String, Object> params) {
		List<AlertSeverityConf> severityList = this.queryList(params);
		if( Tool.CollUtil.isEmpty(severityList) ) {
			return Tool.MapUtil.empty();
		}
		Map<Integer, AlertSeverityConf> severityIdMap = severityList.stream()
				.collect(Collectors.toMap(AlertSeverityConf::getId, a -> a, (k1, k2) -> k1));
		return severityIdMap;
	}

}
