package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@TableName("monitor_endpoint_agent")
@AllArgsConstructor
@NoArgsConstructor
public class MonitorEndpointAgentMap implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer endpointId;

    private Integer agentId;

}