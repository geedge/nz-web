package com.nis.modules.sys.entity;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户查询记录实体
 */
@Data
@TableName("sys_user_query_history")
public class SysUserQueryHistoryEntity implements Serializable {


    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    /**
     * 用户ID
     */
    private Integer uid;

    /**
     * 查询 key
     */
    private String queryKey;

    /**
     * 查询 value
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String queryValue;

    /**
     * value hash
     */
    private String queryHash;

    /**
     * value 统计
     */
    @TableField(exist = false)
    private Integer cnt;

    /**
     * 更新时间戳
     */
    private Long uts;
}
