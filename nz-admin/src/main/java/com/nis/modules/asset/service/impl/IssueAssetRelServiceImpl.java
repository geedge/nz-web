package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.asset.dao.IssueAssetRelDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.IssueAssetRel;
import com.nis.modules.asset.service.IssueAssetRelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IssueAssetRelServiceImpl extends ServiceImpl<IssueAssetRelDao, IssueAssetRel> implements IssueAssetRelService {

    @Autowired
    private IssueAssetRelDao issueAssetRelDao;

    @SuppressWarnings("unchecked")
    @Override
    public List<AssetAsset> queryAssetsByIssueId(Integer issueId) {
        return issueAssetRelDao.queryAssetList(issueId);
    }

}
