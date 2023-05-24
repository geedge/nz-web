package com.nis.modules.tool.service;

import com.nis.common.utils.PageUtils;

import java.util.Map;

/**
 * 高级搜索
 * FullTextSearchService
 *
 * @author shizhendong
 * @date 2023-05-24 17:31:44.352
 */
public interface FullTextSearchService {

    /**
     * 元数据查询
     * 定义各个页面支持的搜索元数据配置
     *
     * @param type
     * @return
     */
    Object queryMetadata(String type);

    /**
     * keyword 查询
     *
     * @param metadataType
     * @param text
     * @param pageNo
     * @param pageSize
     * @return
     */
    PageUtils queryKeywordList(String metadataType, String text, String pageNo, String pageSize);

    /**
     * alertMessage 高级搜索
     *
     * @param params
     * @return
     */
    PageUtils queryAlertMessageFullTextPage(Map<String, Object> params);

}
