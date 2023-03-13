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
     * 子网名称
     */
    private String name;

    /**
     * ip类型
     */
    private Integer type;

    /**
     * 子网地址
     */
    private String addr;

    /**
     * 子网掩码
     */
    private Integer mask;
    
    /**
     * vlan名称
     */
    private String vlan;
    
    /**
     * 总容量
     */
    private String cap;
    
    /**
     * 数据中心id
     */
    private Integer dcId;
    
    /**
     * 备注
     */
    private String remark;
    
    private String seq;
    
    @TableField(exist = false)
    private Dc dc;
    
    @TableField(exist = false)
    private Integer used;

    /**
     * 是否收藏 0:未收藏 1:已收藏
     */
    @TableField(exist = false)
    private Integer starred;
}
