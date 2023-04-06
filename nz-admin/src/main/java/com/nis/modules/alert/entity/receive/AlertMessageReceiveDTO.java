package com.nis.modules.alert.entity.receive;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import com.nis.common.utils.Tool;

import cn.hutool.crypto.SecureUtil;
import lombok.Data;

/**
 * 用来接收告警信息的DTO类
 */
@Data
@SuppressWarnings("all")
public class AlertMessageReceiveDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private LinkedHashMap<String,String> labels;

    private Map labelIds;

    private Map annotations;

    private String startsAt;

    private String endsAt;

    private String generatorURL;
    
    private Long acts;

    private String state;
    
    private String value;
    
    /**
     * 获取 标签中 severity_id 的值
     * @return
     */
    public Integer getSeverityId() {
    	return Tool.MapUtil.getInt(labels, "severity_id");
    }
    
//    private Boolean snmptrap;

    public String genHashKey() {
    	// 排除 severity_id，不同告警级别认定为同一告警
    	Map<Object,Object> newHashMap = Tool.MapUtil.newHashMap(true);
    	newHashMap.putAll(labels);
    	newHashMap.remove("severity_id");
    	return SecureUtil.md5(Tool.JSONUtil.toJsonStr(newHashMap));
    }
}
