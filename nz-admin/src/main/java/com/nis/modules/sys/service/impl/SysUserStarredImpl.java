package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.Tool;
import com.nis.modules.alert.service.AlertRuleService;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.endpoint.service.MonitorEndpointService;
import com.nis.modules.module.service.MonitorModuleService;
import com.nis.modules.panel.service.VisualChartService;
import com.nis.modules.panel.service.VisualPanelService;
import com.nis.modules.project.service.MonitorProjectService;
import com.nis.modules.alert.service.RecordRuleService;
import com.nis.modules.sys.dao.SysUserStarredDao;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service("sysUserStarredService")
public class SysUserStarredImpl extends ServiceImpl<SysUserStarredDao, SysUserStarredEntity> implements SysUserStarredService {

    @Autowired
    private SysUserStarredDao sysUserStarredDao;

    @Autowired
    private DcService dc;

    @Autowired
    private MonitorProjectService monitorProjectService;

    @Autowired
    private MonitorModuleService monitorModuleService;

    @Autowired
    private MonitorEndpointService monitorEndpointService;

    @Autowired
    private AlertRuleService alertRuleService;

    @Autowired
    private AssetAssetService assetAssetService;

    @Autowired
    private VisualPanelService visualPanelService;

    @Autowired
    private VisualChartService visualChartService;

    @Autowired
    private RecordRuleService recordRuleService;

    @Override
    public List<SysUserStarredEntity> queryStarred(String type) {
        Long uid = ShiroUtils.getUserId();

        List<SysUserStarredEntity> sysUserStarredEntityList = null;
        if (StringUtils.isNotEmpty(type)) {
            sysUserStarredEntityList = this.list(new LambdaQueryWrapper<SysUserStarredEntity>()
                    .eq(SysUserStarredEntity::getUid, uid)
                    .eq(SysUserStarredEntity::getType, type)
            );
            return getStarredName(sysUserStarredEntityList);
        }
        sysUserStarredEntityList = this.list(new LambdaQueryWrapper<SysUserStarredEntity>().eq(SysUserStarredEntity::getUid, uid));

        return getStarredName(sysUserStarredEntityList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStarred(SysUserStarredEntity sysUserStarredEntity) {
        ValidateUtils.is(sysUserStarredEntity).notNull(RCode.SYS_USER_STARRED_ISNULL)
                .and(sysUserStarredEntity.getType()).notNull(RCode.SYS_USER_STARRED_TYPE_ISNULL)
                .and(sysUserStarredEntity.getTid()).notNull(RCode.SYS_USER_STARRED_TID_ISNULL);
        Long userId = ShiroUtils.getUserId();
        sysUserStarredEntity.setUid(Integer.parseInt(String.valueOf(userId)));
        sysUserStarredEntity.setCts(new Date().getTime());
        this.save(sysUserStarredEntity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStarred(Map<String, Object> params) {
        if (Tool.ObjectUtil.isEmpty(params.get("ids")) && Tool.ObjectUtil.isEmpty(params.get("type"))) {
            throw new NZException(RCode.SYS_USER_STARRED_ID_TYPE_ISNULL);
        }
        if (Tool.ObjectUtil.isNotEmpty(params.get("type"))) {
            ValidateUtils.is(params.get("tid")).notNull(RCode.SYS_USER_STARRED_TID_ISNULL);
        }
        if (Tool.ObjectUtil.isNotEmpty(params.get("ids"))) {
            params.put("ids", params.get("ids").toString().split(","));
        }
        sysUserStarredDao.removeStarred(params);
    }

    public List<SysUserStarredEntity> getStarredName(List<SysUserStarredEntity> sysUserStarredEntityList){
        if (Tool.ObjectUtil.isNotEmpty(sysUserStarredEntityList) && sysUserStarredEntityList.size() > 0){
            for (SysUserStarredEntity sysUserStarredEntity : sysUserStarredEntityList) {
                Integer tid = sysUserStarredEntity.getTid();
                String type = sysUserStarredEntity.getType();
                switch (type){
                    case "datacenter":
                        sysUserStarredEntity.setName(dc.getById(tid).getName());
                        break;
                    case "project":
                        sysUserStarredEntity.setName(monitorProjectService.getById(tid).getName());
                        break;
                    case "module":
                        sysUserStarredEntity.setName(monitorModuleService.getById(tid).getName());
                        break;
                    case "endpoint":
                        sysUserStarredEntity.setName(monitorEndpointService.getById(tid).getName());
                        break;
                    case "alertRule":
                        sysUserStarredEntity.setName(alertRuleService.getById(tid).getName());
                        break;
                    case "asset":
                        sysUserStarredEntity.setName(assetAssetService.getById(tid).getName());
                        break;
                    case "panel":
                        sysUserStarredEntity.setName(visualPanelService.getById(tid).getName());
                        break;
                    case "chart":
                        sysUserStarredEntity.setName(visualChartService.getById(tid).getName());
                        break;
                    case "recordRule":
                        sysUserStarredEntity.setName(recordRuleService.getById(tid).getName());
                        break;
                }
            }
            return sysUserStarredEntityList;
        }
        return null;
    }
}
