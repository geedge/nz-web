package com.nis.modules.vsys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@TableName("vsys_task")
public class VsysTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 任务唯一标识
     */
    private String taskUuid;

    /**
     * 任务类型
     * 1：add
     * 2：update
     * 3：delete
     */
    private Integer type;

    /**
     * vsys id
     */
    private Integer vsysId;

    /**
     * asset Ids
     */
    private String assetIds;
    /**
     * 设备组
     */
    private Object deviceTag;

    /**
     * 参数
     */
    private String params;


    /**
     * obj  name
     */
    @TableField(exist = false)
    private String name;

    /**
     * 分流规则
     */
    @TableField(exist = false)
    private Map classificationRule;

    /**
     * 配额参数
     *
     * @deprecated 未来取消, 后续仅保留 classification rule 配置
     */
    @TableField(exist = false)
    private Map osQuotas;
    /**
     * 并发数	默认： 0 ，不限制
     */
    private Integer concurrency;

    /**
     * 失败重试次数	默认：0
     */
    private Integer retry;

    /**
     * 进度	默认值：0
     */
    private Double progress;

    /**
     * 下发设备总数量
     */
    private Integer total;

    /**
     * 成功数量	默认值：0
     */
    private Integer success;

    /**
     * 失败数量	默认值：0
     */
    private Integer failed;

    /**
     * 时间戳
     */
    private Long ts;


    @TableField(exist = false)
    private List<VsysTask> vsysInfos;


}
