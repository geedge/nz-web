package com.nis.modules.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@TableName("visual_playlist")
public class VisualPlaylist implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;

    /**
     * name
     * max length: 64
     */
    private String name;

    /**
     * 间隔
     * 单位： s
     * 默认：300
     */
    private Integer intvl;

    /**
     * 模式
     * 可选：normal、tv、se
     * 默认：normal
     */
    @TableField(value = "`mode`")
    private String mode;

    /**
     * dashboard id list
     * json 数组，例：[1,2]
     */
    private Object dashboardIds;

    /**
     * update id
     */
    private Integer uid;

    /**
     * update time
     */
    private Long uts;


    @TableField(exist = false)
    private List<Object> dashboards;

    public String getName() {
        return Tool.StrUtil.trim(name);
    }

    public void setName(String name) {
        this.name = Tool.StrUtil.trim(name);
    }
}
