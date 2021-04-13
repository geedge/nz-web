package com.nis.modules.alert.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.alert.dao.AlertSeverityDao;
import com.nis.modules.alert.entity.AlertNotifyMethod;
import com.nis.modules.alert.entity.AlertSeverityConf;
import com.nis.modules.alert.service.AlertNotifyMethodService;
import com.nis.modules.alert.service.AlertSeverityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;


@Service("alertSeverityService")
public class AlertSeverityServiceImpl extends ServiceImpl<AlertSeverityDao, AlertSeverityConf> implements AlertSeverityService {
    private Log logger= Log.get();

    @Autowired
    private AlertSeverityDao alertSeverityDao;

    @Autowired
    private AlertNotifyMethodService alertNotifyMethodService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage page = new Query<AlertSeverityConf>().getPage(params);

        List<AlertSeverityConf> list = alertSeverityDao.queryList(page,params);
        //遍历，设置notify method
        list.stream().forEach(item -> setNotifyMethods(item) );
        page.setRecords(list);

        return new PageUtils(page);
    }

    @Override
    public List<AlertSeverityConf> queryList(Map<String, Object> params) {

        List<AlertSeverityConf> list = this.list(new QueryWrapper<AlertSeverityConf>().lambda()
                                                    .in(ToolUtil.isNotEmpty(params.get("ids")),AlertSeverityConf::getId,String.valueOf(params.get("ids")).split(","))
                                                    .like(ToolUtil.isNotEmpty(params.get("name")),AlertSeverityConf::getName,String.valueOf(params.get("name")))
                                                );

        list.stream().forEach(item -> setNotifyMethods(item) );

        return list;
    }

    @Override
    public void validateSeverity(AlertSeverityConf alertSeverityConf, boolean isModify) {
        ValidateUtils validateUtils = ValidateUtils.is(alertSeverityConf.getName()).notNull(RCode.ALERTSEVERITY_NAME_ISNULL)
                .and(alertSeverityConf.getColor()).notNull(RCode.ALERTSEVERITY_COLOR_ISNULL)
                .and(alertSeverityConf.getMethods()).notNull(RCode.ALERTSEVERITY_METHODS_ISNULL)
                .and(alertSeverityConf.getRemark()).notNull(RCode.ALERTSEVERITY_REMARK_ISNULL);
        if(isModify){
            validateUtils.and(alertSeverityConf.getId()).notNull(RCode.ALERTSEVERITY_ID_ISNULL);
        }
    }

    @Override
    public void setWeight(AlertSeverityConf alertSeverityConf) {
        AlertSeverityConf max = this.getOne(new QueryWrapper<AlertSeverityConf>().select("id","weight").orderByDesc("id").last("limit 1"));
        if(ToolUtil.isEmpty(max)){
            alertSeverityConf.setWeight(0);
        }else{
            alertSeverityConf.setWeight(max.getWeight()+1);
        }
    }

    @Override
    public void saveWeights(List<AlertSeverityConf> alertSeverityConfs) {
        //校验传递的id weight是否为空
        AlertSeverityConf alertSeverityConf = alertSeverityConfs.stream().filter(item -> ToolUtil.isEmpty(item.getId()) || ToolUtil.isEmpty(item.getWeight())).findAny().orElse(null);
        if(alertSeverityConf != null)
            throw new NZException(RCode.ALERTSEVERITY_WEIGHT_PARAM_ISNULL);

        alertSeverityDao.saveWeightBatch(alertSeverityConfs);
    }

    public AlertSeverityConf getSeverityById(Integer id){
        AlertSeverityConf alertSeverityConf = super.getById(id);
        if(alertSeverityConf != null ){
            setNotifyMethods(alertSeverityConf);
        }
        return alertSeverityConf;
    }

    @Override
    public void saveSeverity(AlertSeverityConf alertSeverityConf) {
        //校验是否存在id对应的notify method
        if(!alertNotifyMethodService.isExist(alertSeverityConf.getMethods())){
            throw new NZException(RCode.ALERTNOTIFY_NOT_EXISTS);
        }
        this.setWeight(alertSeverityConf);
        this.save(alertSeverityConf);
    }

    @Override
    public void updateSeverity(AlertSeverityConf alertSeverityConf) {
        //校验是否存在id对应的notify method
        if(!alertNotifyMethodService.isExist(alertSeverityConf.getMethods())){
            throw new NZException(RCode.ALERTNOTIFY_NOT_EXISTS);
        }
        this.updateById(alertSeverityConf);
    }


    /**
    * @Description 为SeverityConf 实体类填充notify method
    * @Author  rui
    * @Date   2021/4/12
    */
    private void setNotifyMethods(AlertSeverityConf conf){
        if(conf.toMethodsIds().size()>0){
            Collection<AlertNotifyMethod> methods = alertNotifyMethodService.listByIds(conf.toMethodsIds());
            conf.setNotifyMethods(Tool.ListUtil.toList(methods));
        }
    }
}
