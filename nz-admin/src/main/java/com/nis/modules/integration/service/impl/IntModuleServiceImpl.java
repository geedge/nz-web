package com.nis.modules.integration.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.integration.dao.IntAlertRuleDao;
import com.nis.modules.integration.dao.IntMetadataDao;
import com.nis.modules.integration.dao.IntModuleDao;
import com.nis.modules.integration.entity.IntAlertRuleEntity;
import com.nis.modules.integration.entity.IntMetadataEntity;
import com.nis.modules.integration.entity.IntModuleEntity;
import com.nis.modules.integration.service.IntModuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service("intModuleService")
public class IntModuleServiceImpl extends ServiceImpl<IntModuleDao, IntModuleEntity> implements IntModuleService {

    private static Log log = Log.get();

    @Autowired
    private IntModuleDao moduleDao;

    @Autowired
    private IntMetadataDao metadataDao;

    @Autowired
    private IntAlertRuleDao intAlertRuleDao;

    @Override
    public IntModuleEntity queryIntModuleInfo(Integer id) {
        IntModuleEntity moduleEntity = moduleDao.queryInfo(id);
        if (ToolUtil.isEmpty(moduleEntity)) {
            throw new NZException(RCode.INT_MODULE_NOT_FOUNT);
        }

        List<IntMetadataEntity> metadataList = metadataDao.selectList(new LambdaQueryWrapper<IntMetadataEntity>().eq(IntMetadataEntity::getIntModuleId, moduleEntity.getId()));
        moduleEntity.setMetadatas(metadataList);

        List<IntAlertRuleEntity> intAlertRuleList = intAlertRuleDao.selectList(new LambdaQueryWrapper<IntAlertRuleEntity>().eq(IntAlertRuleEntity::getIntModuleId, moduleEntity.getId()));
        moduleEntity.setAlertRules(intAlertRuleList);

        return moduleEntity;
    }

    @Override
    public PageUtils queryModulePage(Map param) {

        IPage page = new Query(IntModuleEntity.class).getPage(param);

        List<IntModuleEntity> moduleList = moduleDao.queryList(page, param);

        page.setRecords(moduleList);

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryMetadataPage(Map params) {

        String moduleIds = (String) params.get("moduleIds");
        if (StrUtil.isNotBlank(moduleIds)) {
            params.put("moduleIds", Arrays.asList(moduleIds.split(",")));
        }
        IPage page = new Query(IntMetadataEntity.class).getPage(params);

        List<IntMetadataEntity> metadataEntities = metadataDao.queryList(page, params);

        page.setRecords(metadataEntities);

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryAlertRulePage(Map params) {

        String moduleIds = (String) params.get("moduleIds");
        if (StrUtil.isNotBlank(moduleIds)) {
            params.put("moduleIds", Arrays.asList(moduleIds.split(",")));
        }
        IPage page = new Query(IntAlertRuleEntity.class).getPage(params);
        List<IntAlertRuleEntity> alertRuleEntities = intAlertRuleDao.queryList(page, params);

        page.setRecords(alertRuleEntities);

        return new PageUtils(page);
    }
}
