package com.nis.modules.sys.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.DigestUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.modules.sys.dao.SysUserQueryHistoryDao;
import com.nis.modules.sys.entity.SysUserQueryHistoryEntity;
import com.nis.modules.sys.service.SysUserQueryHistoryService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SysUserQueryHistoryServiceImpl extends ServiceImpl<SysUserQueryHistoryDao, SysUserQueryHistoryEntity> implements SysUserQueryHistoryService {


    @Autowired
    private SysUserQueryHistoryDao sysUserQueryHistoryDao;

    @Override
    public PageUtils queryHistory(Map<String, Object> params) {
        IPage page = new Query(SysUserQueryHistoryEntity.class).getPage(params);
        params.put("uid", ShiroUtils.getUserId().intValue());
        List<SysUserQueryHistoryEntity> queryHistoryEntityList = sysUserQueryHistoryDao.queryList(params, page);
        page.setRecords(queryHistoryEntityList);
        return new PageUtils(page);
    }

    @Override
    public void saveQueryHistory(List<SysUserQueryHistoryEntity> queryHistoryList) {
        for (SysUserQueryHistoryEntity entity : queryHistoryList) {
            String queryValueHash = DigestUtils.md5Hex(entity.getQueryValue());
            entity.setQueryHash(queryValueHash);
            entity.setUid(ShiroUtils.getUserId().intValue());
            entity.setUts(System.currentTimeMillis());
        }
        this.saveBatch(queryHistoryList);
    }

    @Override
    public void deleteQueryHistory(Map params) {
        if (ObjectUtil.isNotEmpty(params.get("ids"))) {
            List<String> ids = StrUtil.split(params.get("ids").toString(), ",");
            params.put("ids", ids);
        }
        sysUserQueryHistoryDao.deleteQueryHistory(params);
    }
}
