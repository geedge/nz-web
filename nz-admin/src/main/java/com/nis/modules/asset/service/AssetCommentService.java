package com.nis.modules.asset.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.common.utils.PageUtils;
import com.nis.modules.asset.entity.AssetComment;

import java.util.Map;

/**
 * 资产评论 service
 */
public interface AssetCommentService extends IService<AssetComment> {

    AssetComment queryCommentInfo(Integer id);

    PageUtils queryCommentPage(Map<String, Object> params);

    Integer saveComment(AssetComment assetComment);

    Integer updateComment(AssetComment assetComment);

    void deleteComment(Integer... ids);

}
