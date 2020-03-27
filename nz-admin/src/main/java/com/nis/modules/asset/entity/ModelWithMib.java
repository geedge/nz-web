package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.overview.entity.AssetStat;
import com.nis.modules.sys.entity.SysDictEntity;
import lombok.Data;

import java.io.Serializable;


/**
 * 资产型号实体用于mib关联查询返回
 *
 * @author yuanxingzhi
 * @date 2020-03-26
 */
@Data
public class ModelWithMib implements Serializable {
    private static final long serialVersionUID = 65456456612675212L;

    /**
     * 主键
     */
    private Integer id;

    /**
     * 型号名称
     */
    private String name;

    /**
     * 厂商
     */
    private String vendor;

    /**
     * 类型
     */
    private String type;

}