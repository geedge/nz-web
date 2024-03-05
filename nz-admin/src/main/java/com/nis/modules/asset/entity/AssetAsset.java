package com.nis.modules.asset.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.nis.modules.agent.entity.Agent;
import com.nis.modules.sys.entity.SysUserEntity;
import com.nis.modules.terminal.utils.TerminalConstant;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * 资产实体
 *
 * @author shizhendong
 * @date 2019-11-21
 */
@Data
@TableName("asset_asset")
public class AssetAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 资产名称 唯一
     */
    private String name;

    /**
     * 资产编号	 不重复
     */
    private String number;

    /**
     * 资产序列化 (唯一)
     */
    private String sn;

    /**
     * 默认：-1 只有type vm = 1可以且必须填写此字段，且 pid.type vmh = 1
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Integer pid;

    /**
     * 资产状态
     */
    private Integer stateId;

    /**
     * 资产类型
     */
    private Integer typeId;

    /**
     * 品牌
     */
    private Integer brandId;

    /**
     * 产品型号ID 与model表关联
     */
    private Integer modelId;

    /**
     * 数据中心id
     */
    private Integer dcId;

    /**
     * 认证协议
     * SSH & TELNET
     * equals AssetType.authProtocol
     */
    @TableField(exist = false)
    private Integer authProtocol;

    /**
     * 认证服务端口
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Integer authProtocolPort;
    
    /**
     * 认证方式
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Integer authType;
    
    /**
     * 用户名
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private	String authUsername;
    
    /**
     * 密码
     */
    @TableField
    private String authPin;
    
    /**
     * 秘钥
     */
    @TableField
    private String authPriKey;
    
    /**
     * 用户名提示
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private String authUserTip;
    
    /**
     * 密码提示
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private String authPinTip;

    /**
     * snmp凭证id
     */
    private Integer snmpCredentialId;
    
    /**
     * 机柜编号
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Integer cabinetId;

    /**
     * 机柜开始位置
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Integer cabinetStart;

    /**
     * 机柜结束位置
     */
    @TableField(updateStrategy = FieldStrategy.IGNORED, insertStrategy = FieldStrategy.IGNORED)
    private Integer cabinetEnd;

    /**
     * 管理ip
     */
    private String manageIp;

    private String ipmiIp;

    /**
     * 购买日期
     */
    private String purchaseDate;

    /**
     * talon 连接ip 默认："{{asset.manageIp}}"
     */
    @TableField(value = "client_host")
    private String clientAddress;

    /**
     * talon 连接port  默认：10092
     */
    private Integer clientPort;

    /**
     * talon 连接 用户名
     */
    private String clientUser;

    /**
     * talon 连接 密码
     */
    private String clientPin;
    
    /**
     * talon 连接 token
     */
    private String clientToken;

    /**
     * talon 是否开启
     */
    private Integer clientEnable;
    
    /**
     * 添加日期
     */
    private Date createAt;

    /**
     * 更新日期
     */
    private Date updateAt;


    /**
     * 添加人 user表关联
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 用于撤销具体某次批量导入的uuid
     */
    private String seq;


    /**
     * 关联 asset type 对象
     */
    @TableField(exist = false)
    private AssetTypeConf type;

    /**
     * 关联 asset state 对象
     */
    @TableField(exist = false)
    private AssetStateConf state;

    /**
     * 品牌
     */
    @TableField(exist = false)
    private AssetBrand brand;

    /**
     * 数据中心
     */
    @TableField(exist = false)
    private Dc dc;

    /**
     * 创建人
     */
    @TableField(exist = false)
    private SysUserEntity createUser;

    /**
     * 更新人
     */
    @TableField(exist = false)
    private SysUserEntity updateUser;


    /**
     * 资产型号
     */
    @TableField(exist = false)
    private AssetModel model;

    /**
     * asset field 信息
     */
    @TableField(exist = false)
    private List<AssetFieldMeta> fields;

    /**
     * 机柜
     */
    @TableField(exist = false)
    private Cabinet cabinet;

    /**
     * 虚拟机宿主
     */
    @TableField(exist = false)
    private AssetAsset parent;

    /**
     * 账号信息 ids
     */
    @TableField(exist = false)
    private List<Integer> accounts;

    /**
     * agent
     */
    @TableField(exist = false)
    private Agent agent;

    /**
     * blackbox job scrape_interval
     */
    @TableField(exist = false)
    private String assetPingInterval;
    
    @TableField(exist = false)
    private AssetPing pingInfo;

    @TableField(exist = false)
    private Integer endpointNum;

    @TableField(exist = false)
    private Integer alertNum;

    @TableField(exist = false)
    private Integer childrenNum;
    
    @TableField(exist = false)
    private List<AssetAsset> children;

    @TableField(exist = false)
    private Map<String, Object> license;
    
    public String getPrivateKeyPath() {
        if (this.authType != null && this.authUsername != null) {
            return TerminalConstant.NZ_USER_HOME.concat("/").concat(this.manageIp).concat("@").concat(this.authUsername).concat("_id_rsa");
        }
        return null;
    }

    /**
     * client 状态 关联 asset_talon_status
     */
    @TableField(exist = false)
    private Integer clientState;

    @TableField(exist = false)
    private List alert;

    /**
     * 是否收藏 0:未收藏 1:已收藏
     */
    @TableField(exist = false)
    private Integer starred;

    /**
     * asset.modules 模板变量
     */
    @TableField(exist = false)
    private String modules;

    /**
     * asset basic info
     */
    @TableField(exist = false)
    private Map<String, Object> feature;

}
