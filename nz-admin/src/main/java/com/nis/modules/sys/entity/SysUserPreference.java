package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * System User Preference Entity
 *
 * @author shizhendong
 * @date 2023-08-09
 */
@Data
@TableName("sys_user_preference")
public class SysUserPreference implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Long id;

    /**
     * 用户ID
     */
    private Integer uid;

    /**
     * 偏好配置项
     */
    private String prefKey;

    /**
     * 偏好配置值
     */
    private String prefValue;

    /**
     * 更新时间戳
     */
    private Long uts;

}
