package com.nis.common.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetFieldMeta;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
import com.nis.modules.module.entity.MonitorModule;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;


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
	public static String renderExpression(String expression,Object obj) {
				
		String[] expres = parseExpre(expression);
		if(ToolUtil.isEmpty(expres) || ToolUtil.isEmpty(obj) ) {
			return expression;
		}

		for(String expre : expres) {
			Object renderValue =handleExpre(expre,obj);
			if (renderValue == null) {
	            continue;
	        }
	        expression = expression.replaceAll("\\{\\{" + expre + "\\}\\}", renderValue == null ? "" : renderValue.toString());	    
		}
		return expression;
	}
	
	public static Object handleExpre(String expre,Object obj) {
		Object result = null;
		if(obj instanceof MonitorEndpoint) {
			expre = expre.replaceAll("endpoint.", "");
			result = BeanUtil.getProperty(obj,expre);
		}else if(obj instanceof AssetAsset) {
			expre = expre.replaceAll("asset.", "").replaceAll("datacenter.", "dc.");
			if(!expre.contains("label.")) {
				result = BeanUtil.getProperty(obj,expre);
			}
			if(ToolUtil.isEmpty(result)) {
				// 针对用户输入的变量为field时  特殊处理
				List<AssetFieldMeta> fields = BeanUtil.getProperty(obj, "fields");
				if(ToolUtil.isNotEmpty(fields)) {
					Map<String, String> map = fields.stream().collect(Collectors.toMap(AssetFieldMeta::getMetaKey, AssetFieldMeta::getValue));
					if(expre.contains(StrUtil.DOT)) {
						expre = StrUtil.split(expre, StrUtil.DOT)[1];
					}
					result = map.get(expre);
				}
			}
		}
		return result;
	}
}
