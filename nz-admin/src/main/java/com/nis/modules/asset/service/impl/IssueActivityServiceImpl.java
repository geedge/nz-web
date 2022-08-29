package com.nis.modules.asset.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.IssueActivityDao;
import com.nis.modules.asset.entity.IssueActivity;
import com.nis.modules.asset.service.IssueActivityService;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class IssueActivityServiceImpl extends ServiceImpl<IssueActivityDao, IssueActivity> implements IssueActivityService {

    @Autowired
    private IssueActivityDao issueActivityDao;

    @Override
    public PageUtils queryIssueActivityPage(Map<String, Object> params) {
        ValidateUtils.is(params.get("issueId")).notNull(RCode.ISSUE_ID_ISNULL)
                .and(params.get("type")).notNull(RCode.ISSUE_ACTIVITY_TYPE_ISNULL);
        IPage page = new Query(IssueActivity.class).getPage(params);
        List<IssueActivity> issueList = issueActivityDao.queryIssueActivityList(page, params);
        page.setRecords(issueList);
        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveIssueActivity(IssueActivity issueActivity) {
        // 属性校验
        ValidateUtils.is(issueActivity.getIssueId()).notNull(RCode.ISSUE_ID_ISNULL)
                .and(issueActivity.getContent()).notNull(RCode.ISSUE_ACTIVITY_CONTENT_ISNULL);
        // 设置属性值
        issueActivity.setType(2);
        if(ToolUtil.isEmpty(issueActivity.getReplyId())) issueActivity.setReplyId(-1);
        issueActivity.setCts(System.currentTimeMillis());
        issueActivity.setUts(System.currentTimeMillis());
        issueActivity.setCid(ShiroUtils.getUserId().intValue());
        issueActivity.setUid(ShiroUtils.getUserId().intValue());
        // 保存
        this.save(issueActivity);
        return issueActivity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateIssueActivity(IssueActivity issueActivity) {
        // 属性校验
        ValidateUtils.is(issueActivity.getId()).notNull(RCode.ISSUE_ACTIVITY_ID_ISNULL)
            .and(issueActivity.getContent()).notNull(RCode.ISSUE_ACTIVITY_CONTENT_ISNULL);
        // 只允许修改自己的、comment 类型的动态
        IssueActivity oldIssueActivity = this.getById(issueActivity.getId());
        if(!ToolUtil.equals(2, oldIssueActivity.getType()) || !ToolUtil.equals(ShiroUtils.getUserId().intValue(), oldIssueActivity.getCid())){
            throw new NZException(RCode.ISSUE_ACTIVITY_ONLYOWN_COMMENTS);
        }
        // 设置属性值
        issueActivity.setUts(System.currentTimeMillis());
        issueActivity.setUid(ShiroUtils.getUserId().intValue());
        // 修改
        this.updateById(issueActivity);
        return issueActivity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIssueActivity(Integer... ids) {
        ValidateUtils.is(ids).notNull(RCode.ISSUE_ACTIVITY_ID_ISNULL);
        List<Integer> idList = new ArrayList<>(Arrays.asList(ids));
        // 只允许删除自己的、comment 类型的动态
        int notOwnCommentCount = this.count(new LambdaQueryWrapper<IssueActivity>().in(IssueActivity::getId, idList)
            .and(wrapper ->
                wrapper.ne(IssueActivity::getCid, ShiroUtils.getUserId().intValue())
                .or()
                .ne(IssueActivity::getType, 2))
            );
        if(notOwnCommentCount > 0){
            throw new NZException(RCode.ISSUE_ACTIVITY_ONLYOWN_COMMENTS);
        }
        // 删除 issueActivity
        this.removeByIds(idList);
    }

}
