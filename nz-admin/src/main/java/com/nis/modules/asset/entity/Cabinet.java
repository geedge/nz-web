package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;


/**
 * 机柜实体
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Data
@TableName("cabinet")
public class Cabinet implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;

    /**
     * 机柜名称
     */
    private String name;

    /**
     * 数据中心id
     */
    private Integer idcId;

    /**
     * u位容量
     */
    private Integer uSize;

    /**
     * 备注
     */
    private String remark;

    /**
     * 用于撤销具体某次批量导入的uuid
     */
    private String seq;

    // 解决lombok自动生成方法命名问题
    public Integer getuSize() {
        return uSize;
    }

    public void setuSize(Integer uSize) {
        this.uSize = uSize;
    }
}