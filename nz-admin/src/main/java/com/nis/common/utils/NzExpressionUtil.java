package com.nis.common.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.module.entity.MonitorModule;
import com.nis.modules.project.entity.MonitorProject;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NzExpressionUtil {
	 
	/**
	 * 获取表达式中的所有变量名
	 * @param expre
	 * @return
	 */
	public static String[] parseExpre(String expre) {
        Pattern pattern = Pattern.compile("(\\{\\{)(.+?)(\\}\\})");
        Matcher matcher = pattern.matcher(expre);

        Set<String> set = new HashSet<>();
        while (matcher.find()) {
            String group = matcher.group();
            group = group.replaceAll("(\\{\\{)|(\\}\\})", "").trim();
            set.add(group);
        }
        String[] vars = set.toArray(new String[] {});
        return vars;
    }
	
	/**
	 * 替换表达式中的标量
	 * @param expression   表达式 
	 * @param type         默认变量类型
	 * @param obj          传入变量值 asset endpoint
	 * @return
	 */
	public static String renderExpression(String expression,Object... obj) {
				
		String[] expres = parseExpre(expression);
		if(ToolUtil.isEmpty(expres) || ToolUtil.isEmpty(obj) ) {
			return expression;
		}

		for(String expre : expres) {
			Object renderValue =handleExpre(expre,obj);
			if (renderValue == null) {
	            continue;
	        }
			expre = expre.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
	        expression = expression.replaceAll("\\{\\{" + expre + "\\}\\}", renderValue == null ? "\\{\\{" + expre + "\\}\\}" : renderValue.toString());	    
		}
		return expression;
	}
	
	public static Object handleExpre(String expre,Object... objs) {
		Object result = null;
		for(Object obj : objs) {
			if(obj instanceof MonitorEndpoint) {
				expre = expre.replaceAll("endpoint.", "");
				result = BeanUtil.getProperty(obj,expre);
				if(ToolUtil.isEmpty(result)&&expre.contains("asset.")) {
					// 针对用户输入的变量为field时  特殊处理
					List<AssetFieldMeta> fields = BeanUtil.getProperty(obj, "asset.fields");
					if(ToolUtil.isNotEmpty(fields)) {
						String key=null;
						Integer index=null;
						Map<String, List<String>> map = fields.stream().collect(Collectors.toMap(AssetFieldMeta::getMetaKey, AssetFieldMeta::getValue));
						Map<String, String> keyTypeMap = fields.stream().collect(Collectors.toMap(AssetFieldMeta::getMetaKey, AssetFieldMeta::getType));
						if(expre.contains(StrUtil.DOT)) {
							expre = StrUtil.split(expre, StrUtil.DOT)[1];
						}
						try {
							Pattern pattern = Pattern.compile("(\\[)(.+?)(\\])");
					        Matcher matcher = pattern.matcher(expre);
					        while (matcher.find()) {
						        String group = matcher.group();
						        group=group.replaceAll("(\\[)|(\\])", "").trim();
						        index = Integer.valueOf(group);
						        key=expre.substring(0,expre.indexOf("["));
					        }
						}catch(NumberFormatException e) {
					        
					    }
						// 用来取值的 metakey
						String valueKey = Tool.StrUtil.isEmpty(key) ? expre : key;
						String type = keyTypeMap.get(valueKey);
						// 当类型为 MULTITEXT CHECKBOX 需要通过下标进行取值
						if (Tool.StrUtil.equalsIgnoreCase("MULTITEXT", type) || Tool.StrUtil.equalsIgnoreCase("CHECKBOX", type) || Tool.StrUtil.equalsIgnoreCase("DATETIME", type)) {
							if (ToolUtil.isNotEmpty(map) && ToolUtil.isNotEmpty(key) && ToolUtil.isNotEmpty(index)) {
								List<String> list = map.get(key);
								if (ToolUtil.isNotEmpty(list)) {
									result = index >= list.size() ? "" : list.get(index);
								}
							} else {
								result = map.get(expre);
							}
						} else {
							// 其他类型为 一对一 直接取第一个即可
							List<String> list = map.get(valueKey);
							if (Tool.CollectionUtil.isNotEmpty(list)) {
								result = list.get(0);
							} else {
								result = null;
							}
						}
					}
				}
			}else if(obj instanceof AssetAsset) {
				expre = expre.replaceAll("asset.", "").replaceAll("datacenter.", "dc.");
				if(!expre.contains("label.")) {
					result = BeanUtil.getProperty(obj,expre);
				}
				if(ToolUtil.isEmpty(result)) {
					// 针对用户输入的变量为field时  特殊处理
					List<AssetFieldMeta> fields = BeanUtil.getProperty(obj, "fields");
					if(ToolUtil.isNotEmpty(fields)) {
						String key=null;
						Integer index=null;
						Map<String, List<String>> map = fields.stream().collect(Collectors.toMap(AssetFieldMeta::getMetaKey, AssetFieldMeta::getValue));
						Map<String, String> keyTypeMap = fields.stream().collect(Collectors.toMap(AssetFieldMeta::getMetaKey, AssetFieldMeta::getType));
						if(expre.contains(StrUtil.DOT)) {
							expre = StrUtil.split(expre, StrUtil.DOT)[1];
						}
						try {
							Pattern pattern = Pattern.compile("(\\[)(.+?)(\\])");
					        Matcher matcher = pattern.matcher(expre);
					        while (matcher.find()) {
						        String group = matcher.group();
						        group=group.replaceAll("(\\[)|(\\])", "").trim();
						        index = Integer.valueOf(group);
						        key=expre.substring(0,expre.indexOf("["));
					        }
						}catch(NumberFormatException e) {
					        
					    }
						// 用来取值的 metakey
						String valueKey = Tool.StrUtil.isEmpty(key) ? expre : key;
						String type = keyTypeMap.get(valueKey);
						// 当类型为 MULTITEXT CHECKBOX 需要通过下标进行取值
						if (Tool.StrUtil.equalsIgnoreCase("MULTITEXT", type) || Tool.StrUtil.equalsIgnoreCase("CHECKBOX", type) || Tool.StrUtil.equalsIgnoreCase("DATETIME", type)) {
							if (ToolUtil.isNotEmpty(map) && ToolUtil.isNotEmpty(key) && ToolUtil.isNotEmpty(index)) {
								List<String> list = map.get(key);
								if (ToolUtil.isNotEmpty(list)) {
									result = index >= list.size() ? "" : list.get(index);
								}
							} else {
								result = map.get(expre);
							}
						} else {
							// 其他类型为 一对一 直接取第一个即可
							List<String> list = map.get(valueKey);
							if (Tool.CollectionUtil.isNotEmpty(list)) {
								result = list.get(0);
							} else {
								result = null;
							}
						}
					}
				}
			}else if(obj instanceof MonitorProject) {
				expre = expre.replaceAll("project.", "");
				result = BeanUtil.getProperty(obj,expre);
			}else if (obj instanceof MonitorModule) {
				expre = expre.replaceAll("module.", "");
				result = BeanUtil.getProperty(obj,expre);
			}else if (obj instanceof Dc) {
				expre = expre.replaceAll("datacenter.", "").replaceAll("dc.", "");
				result = BeanUtil.getProperty(obj,expre);
			}else if (obj instanceof Map) {
				result = BeanUtil.getProperty(obj, expre);
			}
			if(ToolUtil.isNotEmpty(result)) {
				break;
			}
		}
		return result;
	}
}
