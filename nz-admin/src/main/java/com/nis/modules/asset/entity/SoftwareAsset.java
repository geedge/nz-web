package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.common.utils.Tool;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * software asset entity
 *
 * @author shizhendong
 * @date 2023-08-10
 */
@Data
@TableName("software_asset")
public class SoftwareAsset implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId
    private Integer id;

    /**
     * 名称
     */
    private String name;

    /**
     * 硬件资产id
     */
    private Integer assetId;

    /**
     * 软件类型ID
     */
    private Integer typeId;

    /**
     * 配置参数
     */
    private Object params;

    /**
     *签发时间,时间戳,单位：ms
     */
    private Long dateIssued;

    /**
     * 失效时间,时间戳,单位：ms
     */
    private Long dateExpires;

    /**
     * 授权数量,type = TSG-OS Concentrate 时有效
     */
    private Integer concurrency;

    /**
     * 已登录数量,type = TSG-OS Concentrate 时有效
     */
    private Integer logins;

    /**
     * 授权状态, 0：未授权,1：已授权,2：未知错误（网络无法访问）
     */
    private Integer status;

    /**
     * 描述
     */
    private String remark;

    /**
     * 是否内置
     * 1:内置，0：非内置，默认：0
     * 内置数据不允许删除
     */
    private Integer buildIn;

    /**
     * 添加时间戳
     */
    private Long cts;

    /**
     * 添加用户 id
     */
    private Integer cid;

    /**
     * 更新时间戳
     */
    private Long uts;

    /**
     * 更新用户 id
     */
    private Integer uid;

    /**
     * 锁 ID
     */
    private String haspId;

    /**
     * 最后扫描时间,单位：ms
     */
    private Long lastScanTs;
    
    /**
     * 最后一次扫描错误信息
     */
    private String lastScanMsg;


    @TableField(exist = false)
    private AssetAsset asset;

    @TableField(exist = false)
    private SoftwareType type;

    public void setName(String name) {
        this.name = Tool.StrUtil.trim(name);
    }


}
