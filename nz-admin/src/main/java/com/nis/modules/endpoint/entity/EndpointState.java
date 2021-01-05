package com.nis.modules.endpoint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("endpoint_state")
public class EndpointState implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id",type = IdType.INPUT)
    private Integer id;
	private Integer endpointId;
	private Date createTime;
	private Integer state;

    public EndpointState() {
    }

    public EndpointState(Integer id, Integer endpointId, Date createTime, Integer state) {
        this.id = id;
        this.endpointId = endpointId;
        this.createTime = createTime;
        this.state = state;
    }
}
