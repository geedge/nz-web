package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.IssueRecordDao;
import com.nis.modules.asset.entity.IssueActivity;
import com.nis.modules.asset.entity.IssueAssetRel;
import com.nis.modules.asset.entity.IssueRecord;
import com.nis.modules.asset.service.IssueActivityService;
import com.nis.modules.asset.service.IssueAssetRelService;
import com.nis.modules.asset.service.IssueRecordService;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 资产问题    serviceImpl
 */
@Service
public class IssueRecordServiceImpl extends ServiceImpl<IssueRecordDao, IssueRecord> implements IssueRecordService {

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private IssueRecordDao issueRecordDao;

    @Autowired
    private IssueAssetRelService issueAssetRelService;
    @Autowired
    private IssueActivityService issueActivityService;

    @Autowired
    private SysUserStarredService starredService;

    @SuppressWarnings("unchecked")
    @Override
    public IssueRecord queryIssueInfo(Integer id) {
        if (ToolUtil.isEmpty(id)) {
            throw new NZException(RCode.ISSUE_ID_ISNULL);
        }
        IssueRecord issueRecord = this.getById(id);

        if (issueRecord.getCid() != null) {
            SysUserEntity userEntity = sysUserDao.selectById(issueRecord.getCid());
            issueRecord.setCreateUser(userEntity);
        }

        if (issueRecord.getUid() != null) {
            SysUserEntity userEntity = sysUserDao.selectById(issueRecord.getUid());
            issueRecord.setUpdateUser(userEntity);
        }

        if (issueRecord.getRid() != null) {
            SysUserEntity userEntity = sysUserDao.selectById(issueRecord.getRid());
            issueRecord.setReporter(userEntity);
        }

        if (issueRecord.getAid() != null) {
            SysUserEntity userEntity = sysUserDao.selectById(issueRecord.getAid());
            issueRecord.setAssignee(userEntity);
        }
        // 相关资产
        issueRecord.setAssetIds(issueAssetRelService.queryAssetsIds(id));
        issueRecord.setAssets(issueAssetRelService.queryAssetsByIssueId(id));
        // 收藏标识 0：未收藏 1：已收藏
        int count = starredService.count(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getTid,id)
                .eq(SysUserStarredEntity::getType,"issueRecord")
                .eq(SysUserStarredEntity::getUid,ShiroUtils.getUserId())
        );
        issueRecord.setStarred(count == 0 ? 0 : 1);
        return issueRecord;
    }

    @Override
    public PageUtils queryIssuePage(Map<String, Object> params) {
        // 处理请求参数
        String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids",listIds);
        }
        //当前用户id
        params.put("userId",ShiroUtils.getUserId().intValue());

        IPage page = new Query(IssueRecord.class).getPage(params);
        List<IssueRecord> issueList = issueRecordDao.queryIssueList(page, params);

        // 查询结果不为空时处理返回结果
        if (CollectionUtils.isNotEmpty(issueList)) {
            this.handleResuleList(issueList);
        }

        page.setRecords(issueList);
        return new PageUtils(page);
    }

    @SuppressWarnings("unchecked")
	private void handleResuleList(List<IssueRecord> issueList) {
        List<SysUserEntity> userList = sysUserDao.selectList(null);
        Map<Long, SysUserEntity> userIdAndEntityMap = userList.stream().collect(Collectors.toMap(SysUserEntity::getId, Function.identity()));

        for (IssueRecord issueRecord : issueList) {
            if (ToolUtil.isNotEmpty(issueRecord.getCid())) {
                issueRecord.setCreateUser(userIdAndEntityMap.get(issueRecord.getCid().longValue()));
            }
            if (ToolUtil.isNotEmpty(issueRecord.getUid())) {
                issueRecord.setUpdateUser(userIdAndEntityMap.get(issueRecord.getUid().longValue()));
            }
            if (ToolUtil.isNotEmpty(issueRecord.getRid())) {
                issueRecord.setReporter(userIdAndEntityMap.get(issueRecord.getRid().longValue()));
            }
            if (ToolUtil.isNotEmpty(issueRecord.getAid())) {
                issueRecord.setAssignee(userIdAndEntityMap.get(issueRecord.getAid().longValue()));
            }
        }
    }

    private void validateIssueInfo(IssueRecord issueRecord) {
        ValidateUtils.is(issueRecord.getName()).notNull(RCode.ISSUE_NAME_ISNULL).maxLength(256)
            .and(issueRecord.getType()).notNull(RCode.ISSUE_TYPE_ISNULL).maxLength(256)
            .and(issueRecord.getContent()).notNull(RCode.ISSUE_CONTENT_ISNULL)
            .and(issueRecord.getState()).notNull(RCode.ISSUE_STATE_ISNULL)
            .and(issueRecord.getRid()).notNull(RCode.ISSUE_RID_ID_ISNULL)
            .and(issueRecord.getAid()).notNull(RCode.ISSUE_AID_ID_ISNULL);

        Integer issueId = issueRecord.getId();
        // name 唯一
        int duplicateCount = this.count(new LambdaQueryWrapper<IssueRecord>().eq(IssueRecord::getName, issueRecord.getName()).ne(ToolUtil.isNotEmpty(issueId), IssueRecord::getId, issueId));
        if (duplicateCount > 0) {
            throw new NZException(RCode.ISSUE_NAME_DUPLICATE);
        }
        if(!Tool.CollUtil.contains(Arrays.asList(new Integer[]{1,2,3,4,5,6,7}), issueRecord.getState())){
            throw new NZException(RCode.ISSUE_STATE_INVALID);
        }
        if(ToolUtil.isEmpty(issueRecord.getPriority())){
            issueRecord.setPriority(2);
        } else if (!Tool.CollUtil.contains(Arrays.asList(new Integer[]{1,2,3}), issueRecord.getPriority())){
            throw new NZException(RCode.ISSUE_PRIORITY_INVALID);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveIssue(IssueRecord issueRecord) {
        issueRecord.setId(null);
        // 属性校验
        issueRecord.setName(StrUtil.trim(issueRecord.getName()));
        this.validateIssueInfo(issueRecord);
        // 设置属性值
        issueRecord.setCts(System.currentTimeMillis());
        issueRecord.setUts(System.currentTimeMillis());
        issueRecord.setCid(ShiroUtils.getUserId().intValue());
        issueRecord.setUid(ShiroUtils.getUserId().intValue());
        // 保存
        this.save(issueRecord);
        // 问题动态更新
        this.saveIssueActivityChange(issueRecord, null);
        // 保存关联关系
        if(Tool.CollectionUtil.isNotEmpty(issueRecord.getAssetIds())){
            List<IssueAssetRel> issueAssetRelList = new ArrayList<>();
            for (Integer assetId : issueRecord.getAssetIds()) {
                IssueAssetRel issueAssetRel = new IssueAssetRel();
                issueAssetRel.setIssueId(issueRecord.getId());
                issueAssetRel.setAssetId(assetId);
                issueAssetRelList.add(issueAssetRel);
            }
            issueAssetRelService.saveBatch(issueAssetRelList);
        }
        return issueRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateIssue(IssueRecord issueRecord) {
        // 属性校验
        issueRecord.setName(StrUtil.trim(issueRecord.getName()));
        this.validateIssueInfo(issueRecord);
        // 设置属性值
        issueRecord.setUts(System.currentTimeMillis());
        issueRecord.setUid(ShiroUtils.getUserId().intValue());
        IssueRecord oldIssueRecord = this.getById(issueRecord.getId());
        // 修改
        this.updateById(issueRecord);
        // 问题动态更新
        this.saveIssueActivityChange(issueRecord, oldIssueRecord);
        // 先删除之前的asset关联
        issueAssetRelService.remove(new LambdaQueryWrapper<IssueAssetRel>().eq(IssueAssetRel::getIssueId, issueRecord.getId()));
        // 保存asset关联
        if(Tool.CollectionUtil.isNotEmpty(issueRecord.getAssetIds())){
            List<IssueAssetRel> issueAssetRelList = new ArrayList<>();
            for (Integer assetId : issueRecord.getAssetIds()) {
                IssueAssetRel issueAssetRel = new IssueAssetRel();
                issueAssetRel.setIssueId(issueRecord.getId());
                issueAssetRel.setAssetId(assetId);
                issueAssetRelList.add(issueAssetRel);
            }
            issueAssetRelService.saveBatch(issueAssetRelList);
        }
        return issueRecord.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIssue(Integer... ids) {
        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        //删除starred
        starredService.remove(new LambdaQueryWrapper<SysUserStarredEntity>()
                .eq(SysUserStarredEntity::getType,"issueRecord")
                .in(SysUserStarredEntity::getTid,idList)
        );
        // 删除asset关联
        issueAssetRelService.remove(new LambdaQueryWrapper<IssueAssetRel>().in(IssueAssetRel::getIssueId, idList));
        // 删除 issueRecord
        this.removeByIds(idList);
    }

    @Override
    public List queryType(String name) {
        List<IssueRecord> issueList = this.list(new LambdaQueryWrapper<IssueRecord>().like(StrUtil.isNotEmpty(name), IssueRecord::getType, name).groupBy(IssueRecord::getType));
        return issueList.stream().map(IssueRecord::getType).collect(Collectors.toList());
    }

    /**
     * 问题新增修改时，记录 原配置内容 及 新配置内容到 问题动态表
     * @param newIssueRecord
     * @param oldIssueRecord
     */
    public void saveIssueActivityChange(IssueRecord newIssueRecord, IssueRecord oldIssueRecord){
        IssueActivity issueActivity = new IssueActivity();
        issueActivity.setCts(System.currentTimeMillis());
        issueActivity.setUts(System.currentTimeMillis());
        issueActivity.setCid(ShiroUtils.getUserId().intValue());
        issueActivity.setUid(ShiroUtils.getUserId().intValue());
        issueActivity.setIssueId(newIssueRecord.getId());
        issueActivity.setType(1);
        issueActivity.setReplyId(-1);
        Map content = Tool.MapUtil.newHashMap();
        if(ToolUtil.isNotEmpty(oldIssueRecord)){
            // 相关资产
            oldIssueRecord.setAssetIds(issueAssetRelService.queryAssetsIds(oldIssueRecord.getId()));
            content.put("old", oldIssueRecord);
            newIssueRecord.setCts(oldIssueRecord.getCts());
            newIssueRecord.setCid(oldIssueRecord.getCid());
        }
        content.put("new", newIssueRecord);
        issueActivity.setContent(JSONObject.toJSONString(content));
        issueActivityService.save(issueActivity);
    }

}
