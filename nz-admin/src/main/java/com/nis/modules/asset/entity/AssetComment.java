package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.io.Serializable;

/**
 * 问题管理实体
 */
@Data
@TableName("asset_comment")
public class AssetComment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 资产id
     */
    private Integer assetId;

    /**
     * 回复id
     */
    private Integer replyId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 添加时间戳
     */
    private Long cts;

    /**
     * 更新时间戳
     */
    private Long uts;

    /**
     * 创建人id
     */
    private Integer cid;

    /**
     * 更新人id
     */
    private Integer uid;

    /**
     * 创建人
     */
    @TableField(exist = false)
    private SysUserEntity createUser;

    /**
     * 修改人
     */
    @TableField(exist = false)
    private SysUserEntity updateUser;

    /**
     * 相关资产
     */
    @TableField(exist = false)
    private AssetAsset asset;

}
