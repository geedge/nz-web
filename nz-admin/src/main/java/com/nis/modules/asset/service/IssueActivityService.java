package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.IssueActivity;

import java.util.Map;

/**
 * 资产问题动态 service
 */
public interface IssueActivityService extends IService<IssueActivity> {

    PageUtils queryIssueActivityPage(Map<String, Object> params);

    Integer saveIssueActivity(IssueActivity issueActivity);

    Integer updateIssueActivity(IssueActivity issueActivity);

    void deleteIssueActivity(Integer... ids);

}
