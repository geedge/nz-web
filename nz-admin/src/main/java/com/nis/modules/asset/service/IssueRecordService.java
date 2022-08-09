package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.IssueRecord;

import java.util.List;
import java.util.Map;

/**
 * 资产问题 service
 */
public interface IssueRecordService extends IService<IssueRecord> {

    IssueRecord queryIssueInfo(Integer id);

    PageUtils queryIssuePage(Map<String, Object> params);

    Integer saveIssue(IssueRecord issueRecord);

    Integer updateIssue(IssueRecord issueRecord);

    void deleteIssue(Integer... ids);

    List queryType(String name);
}
