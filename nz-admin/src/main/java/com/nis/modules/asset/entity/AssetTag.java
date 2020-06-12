package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;


/**
 * asset_tag
 *
 * @author shizhendong
 * @date 2020-06-11
 */
@Data
@TableName("asset_tag")
public class AssetTag implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 关联assetId
     */
    private Integer assetId;

    /**
     * 键
     */
    private String tag;

    /**
     * 值
     */
    private String value;

    /**
     * 导入UUID
     */
    private String seq;
}
