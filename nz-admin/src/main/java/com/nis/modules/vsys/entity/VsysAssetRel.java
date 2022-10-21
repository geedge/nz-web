package com.nis.modules.vsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@TableName("vsys_asset_rel")
@NoArgsConstructor
public class VsysAssetRel implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * task id
     */
    private Integer taskId;

    /**
     * end result Id
     */
    private Integer resultId;

    /**
     * 资产id
     */
    private Integer assetId;

    /**
     * vsys id
     */
    private Integer vsysId;

    /**
     * 下发状态
     * 记录下发最终结果
     * 1：成功  0：失败
     */
    private Integer state;

    /**
     * 是否删除
     * 1：是 0：否  默认：0
     */
    private Integer delFlag;

    /**
     * 时间戳
     */
    private Long uts;

    public VsysAssetRel(Integer id, Integer assetId, Integer taskId, Integer vsysId, Integer state, Integer delFlag, Long uts) {
        this.id = id;
        this.assetId = assetId;
        this.taskId = taskId;
        this.vsysId = vsysId;
        this.state = state;
        this.delFlag = delFlag;
        this.uts = uts;
    }

    /**
     * rel asset obj
     */
    @TableField(exist = false)
    private Object asset;

    @TableField(exist = false)
    private VsysTask vsysTask;

    @TableField(exist = false)
    private VsysResult vsysResult;

    @TableField(exist = false)
    private VsysInfo vsysInfo;

}
