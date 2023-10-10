package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("sys_menu")
public class SysMenuEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    private String name;

    // 前端使用 唯一标识 标记按钮或者菜单
    private String code;

    private String i18n;

    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Integer parentId;

    // 后端使用 shiro 权限管理 多个用逗号分隔
    private String perms;

    // 类型 1：菜单 2：按钮
    private Integer type;

    // 路由
    private String route;

    // 排序 默认：1
    private Integer orderNum;

    /**
     * 图标
     */
    private String icon;

    /**
     * 必要权限，如编辑按钮需要 勾选查看权限
     * 填写内容为 sys_menu.id，多个逗号分隔
     */
    private String required;

    /**
     * 状态
     * 默认：1 0:停用 1:启用
     */
    private Integer state;

    @TableField(exist = false)
    private List<SysMenuEntity> children;
}
