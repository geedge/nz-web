package com.nis.modules.alert.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.nis.common.utils.Tool;

import lombok.Data;

@Data
public class Snmptrap{
	private String version;
	private String trapOID;
	private String enterpriseOID;
	private Integer generic;
	private Integer specific;
	private String fromHostName;
	private String source;
	private Integer requestID;
	private String errorStatus;
	private Integer errorIndex;
	private String sysUptime;
	private String message;
	private long timestamp;
	private final Map<String,List<Object>> variables = Tool.MapUtil.newConcurrentHashMap();
	/**
	 * 重新设置 变量值
	 * @param oid
	 * @param value
	 */
	public void setVariable(String oid,Object ... values) {
		if(values != null) {
			ArrayList<Object> list = Tool.ListUtil.toList(values);
			variables.put(oid, list);
		}
	}
	
	/**
	 * 追加 变量值
	 * @param oid
	 * @param values
	 */
	public void appendVariable(String oid,Object ... values) {
		if(values != null) {
			List<Object> list = variables.get(oid);
			if(list == null ) {
				list = Tool.ListUtil.toList(values);
				variables.put(oid, list);
			}else {
				list.addAll(Tool.ListUtil.toList(values));
			}
		}
	}
	
	public String getVariable(String oid) {
		List<Object> list = this.variables.get(oid);
		if(list == null || list.isEmpty()) {
			return Tool.StrUtil.EMPTY;
		}
		return Tool.StrUtil.join(",", list.toArray());
	}
	
	public void delVariable(String oid) {
		this.variables.remove(oid);
	}
}