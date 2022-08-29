package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("issue_activity")
public class IssueActivity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 问题id
     */
    private Integer issueId;

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

    @TableField(exist = false)
    private SysUserEntity createUser;
    @TableField(exist = false)
    private SysUserEntity updateUser;
}
