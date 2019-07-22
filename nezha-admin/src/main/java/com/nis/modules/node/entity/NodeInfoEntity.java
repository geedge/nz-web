package com.nis.modules.node.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.nis.modules.sys.entity.SystemInfoEntity;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 节点信息表
 *
 * @author shizhendong
 * @date 2019-07-18 09:58:18
 */
@TableName("node_info")
public class NodeInfoEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    //表 ID
    private Long id;
    /**
     * 名称
     */
    private String name;
    /**
     * 主机名
     */
    private String hostName;
    /**
     * ip
     */
    private String ip;
    /**
     * ip整型
     */
    private Long ipLong;
    /**
     * 全局唯一
     */
    private Long uuid;
    /**
     * 设备类型，1、server,2、net
     */
    private String type;
    /**
     * 设备标签，与sys_dict关联
     */
    private String tag;
    /**
     * snmpv3 用户名
     */
    private String userName;
    /**
     * snmpv3 密码
     */
    private String password;
    /**
     * 设备状态，1：在线，2：下线，3：删除
     */
    private String state;
    /**
     * 机柜id
     */
    private Long cabinetId;
    /**
     * u位大小
     */
    private Integer uSize;
    /**
     * 机柜起始位置
     */
    private Integer cabinetStart;
    /**
     * snmp默认端口号
     */
    private Integer snmpPort;
    /**
     * snmp团体字
     */
    private String snmpCommunity;
    /**
     * 备注
     */
    private String remark;
    /**
     * 操作人
     */
    private Long operator;
    /**
     * 操作时间
     */
    private Date opTime;




    // 机房名称
    @TableField(exist = false)
    private String roomName;

    //所属机柜
    @TableField(exist = false)
    private NodeCabinetEntity nodeCabinet;

    //所在业务系统
    @TableField(exist = false)
    private List<SystemInfoEntity> systemInfos;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Long getIpLong() {
        return ipLong;
    }

    public void setIpLong(Long ipLong) {
        this.ipLong = ipLong;
    }

    public Long getUuid() {
        return uuid;
    }

    public void setUuid(Long uuid) {
        this.uuid = uuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getCabinetId() {
        return cabinetId;
    }

    public void setCabinetId(Long cabinetId) {
        this.cabinetId = cabinetId;
    }

    public Integer getuSize() {
        return uSize;
    }

    public void setuSize(Integer uSize) {
        this.uSize = uSize;
    }

    public Integer getCabinetStart() {
        return cabinetStart;
    }

    public void setCabinetStart(Integer cabinetStart) {
        this.cabinetStart = cabinetStart;
    }

    public Integer getSnmpPort() {
        return snmpPort;
    }

    public void setSnmpPort(Integer snmpPort) {
        this.snmpPort = snmpPort;
    }

    public String getSnmpCommunity() {
        return snmpCommunity;
    }

    public void setSnmpCommunity(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getOperator() {
        return operator;
    }

    public void setOperator(Long operator) {
        this.operator = operator;
    }

    public Date getOpTime() {
        return opTime;
    }

    public void setOpTime(Date opTime) {
        this.opTime = opTime;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public NodeCabinetEntity getNodeCabinet() {
        return nodeCabinet;
    }

    public void setNodeCabinet(NodeCabinetEntity nodeCabinet) {
        this.nodeCabinet = nodeCabinet;
    }

    public List<SystemInfoEntity> getSystemInfos() {
        return systemInfos;
    }

    public void setSystemInfos(List<SystemInfoEntity> systemInfos) {
        this.systemInfos = systemInfos;
    }

    public NodeInfoEntity() {
    }

    @Override
    public String toString() {
        return "NodeInfoEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", hostName='" + hostName + '\'' +
                ", ip='" + ip + '\'' +
                ", ipLong=" + ipLong +
                ", uuid=" + uuid +
                ", type='" + type + '\'' +
                ", tag='" + tag + '\'' +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", state='" + state + '\'' +
                ", cabinetId=" + cabinetId +
                ", uSize=" + uSize +
                ", cabinetStart=" + cabinetStart +
                ", snmpPort=" + snmpPort +
                ", snmpCommunity='" + snmpCommunity + '\'' +
                ", remark='" + remark + '\'' +
                ", operator=" + operator +
                ", opTime=" + opTime +
                ", roomName='" + roomName + '\'' +
                ", nodeCabinet=" + nodeCabinet +
                ", systemInfos=" + systemInfos +
                '}';
    }
}
