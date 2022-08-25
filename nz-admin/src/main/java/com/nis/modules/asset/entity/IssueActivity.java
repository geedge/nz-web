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

    private String content;
    private Long cts;
    private Long uts;
    private Integer cid;
    private Integer uid;

    @TableField(exist = false)
    private SysUserEntity createUser;
    @TableField(exist = false)
    private SysUserEntity updateUser;
}
