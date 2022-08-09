package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 问题管理实体
 */
@Data
@TableName("issue_record")
public class IssueRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 标题
     */
    private String name;

    /**
     * 类别
     */
    private String type;

    /**
     * 维护内容
     */
    private String content;

    /**
     * 状态：1：打开;2：已指派;3：处理中;4：挂起;5：已解决;6：关闭;7：取消
     */
    private Integer state;

    /**
     * 优先级：1：高;2：中;3：低
     */
    private Integer priority;

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
     * 报告人 reporter id
     */
    private Integer rid;

    /**
     * 指派人 assignee id，默认：-1（代表未指派）
     */
    private Integer aid;

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
     * 报告人
     */
    @TableField(exist = false)
    private SysUserEntity reporter;

    /**
     * 指派人
     */
    @TableField(exist = false)
    private SysUserEntity assignee;

    /**
     * 相关资产
     */
    @TableField(exist = false)
    private List<AssetAsset> assets;

    /**
     * 是否收藏 0:未收藏 1:已收藏
     */
    @TableField(exist = false)
    private Integer starred;

    @TableField(exist = false)
    private List<Integer> assetIds;

}
