package com.nis.modules.sys.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * session 记录
 *
 */

@Data
@TableName("sys_login_session")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SysLoginSession implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private String sessionId;

    /**
     * 用户 id
     */
    private String sessionData;

    /**
     * 最后活跃时间
     */
    private Long lasetActiveTs;

}
