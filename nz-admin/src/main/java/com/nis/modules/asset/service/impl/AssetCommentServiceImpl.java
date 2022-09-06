package com.nis.modules.asset.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.dao.AssetCommentDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetComment;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetCommentService;
import com.nis.modules.sys.dao.SysUserDao;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.sys.shiro.ShiroUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 资产评论    serviceImpl
 */
@Service
public class AssetCommentServiceImpl extends ServiceImpl<AssetCommentDao, AssetComment> implements AssetCommentService {

    @Autowired
    private SysUserDao sysUserDao;

    @Autowired
    private AssetCommentDao assetCommentDao;

    @Autowired
    private AssetAssetService assetService;

    @SuppressWarnings("unchecked")
    @Override
    public AssetComment queryCommentInfo(Integer id) {
        if (ToolUtil.isEmpty(id)) {
            throw new NZException(RCode.ISSUE_ID_ISNULL);
        }
        AssetComment assetComment = this.getById(id);

        if (assetComment.getCid() != null) {
            SysUserEntity userEntity = sysUserDao.selectById(assetComment.getCid());
            assetComment.setCreateUser(userEntity);
        }
        if (assetComment.getUid() != null) {
            SysUserEntity userEntity = sysUserDao.selectById(assetComment.getUid());
            assetComment.setUpdateUser(userEntity);
        }
        if (assetComment.getAssetId() != null) {
            AssetAsset asset = assetService.getById(assetComment.getAssetId());
            assetComment.setAsset(asset);
        }

        return assetComment;
    }

    @Override
    public PageUtils queryCommentPage(Map<String, Object> params) {
        // 处理请求参数
        String ids = (String) params.get("ids");
        if(StrUtil.isNotBlank(ids)) {
            List<String> listIds = Arrays.asList(ids.split(","));
            params.put("ids",listIds);
        }

        IPage page = new Query(AssetComment.class).getPage(params);
        List<AssetComment> commentList = assetCommentDao.queryCommentList(page, params);

        page.setRecords(commentList);
        return new PageUtils(page);
    }

    private void validateCommentInfo(AssetComment assetComment) {
        ValidateUtils.is(assetComment.getAssetId()).notNull(RCode.ASSET_ID_ISNULL)
            .and(assetComment.getContent()).notNull(RCode.ASSET_COMMENT_CONTENT_ISNULL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveComment(AssetComment assetComment) {
        // 属性校验
        this.validateCommentInfo(assetComment);
        // 设置属性值
        assetComment.setCts(System.currentTimeMillis());
        assetComment.setUts(System.currentTimeMillis());
        assetComment.setCid(ShiroUtils.getUserId().intValue());
        assetComment.setUid(ShiroUtils.getUserId().intValue());
        if(ToolUtil.isEmpty(assetComment.getReplyId()))
            assetComment.setReplyId(-1);
        // 保存
        this.save(assetComment);
        return assetComment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateComment(AssetComment assetComment) {
        // 属性校验
        this.validateCommentInfo(assetComment);
        // 设置属性值
        assetComment.setUts(System.currentTimeMillis());
        assetComment.setUid(ShiroUtils.getUserId().intValue());
        // 修改
        this.updateById(assetComment);
        return assetComment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Integer... ids) {
        // 删除 assetComment
        this.removeByIds(Arrays.asList(ids));
    }

}