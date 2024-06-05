package com.nis.modules.ipam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.asset.entity.Dc;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("ipam_subnet")
public class IpamSubnet implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 数据中心id
     */
    private Integer dcId;

    /**
     * IP协议
     */
    private Integer protocol;

    /**
     * 子网类型
     */
    private String type;

    /**
     * 子网地址
     */
    private String addr;

    /**
     * 总容量
     */
    private String cap;
    
    /**
     * 备注
     */
    private String remark;
    
    private String seq;
    
    @TableField(exist = false)
    private Dc dc;

    /**
     * 是否收藏 0:未收藏 1:已收藏
     */
    @TableField(exist = false)
    private Integer starred;
}
