package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * software acc session entity
 *
 * @author shizhendong
 * @date 2023-11-24
 */
@Data
@TableName("software_acc_session")
public class SoftwareAccSession implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    /**
     * 软件名称
     */
    private String said;

    /**
     * ip 地址
     */
    private String ip;


    /**
     * 授权状态
     * 0：未授权
     * 1：已授权
     */
    private Integer status;

}
