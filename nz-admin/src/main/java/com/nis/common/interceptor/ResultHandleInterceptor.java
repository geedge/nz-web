package com.nis.common.interceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.R;
import com.nis.common.utils.ToolUtil;

import cn.hutool.core.util.StrUtil;

@ControllerAdvice
public class ResultHandleInterceptor implements ResponseBodyAdvice {

	private static final String INCLUDE = "include";
	
	private static final String EXCLUDE = "exclude";
	
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter arg1,
                                  MediaType arg2, Class arg3, ServerHttpRequest req,
                                  ServerHttpResponse resp) {
        System.out.println("后端响应的数据信息为"+JSON.toJSONString(body));
        //如果响应值为空则无需过滤
        R result = (R)body;
        Object object = result.get("data");
        if(ToolUtil.isEmpty(object)) {
        	return body;
        }
        HttpHeaders headers = req.getHeaders();
        List<String> list = headers.get("Content-Fields");
        // 判断前端是否传入过滤字段信息
        if(ToolUtil.isEmpty(list)) {
        	return body;
        }
        String contentFiled = list.get(0);
        //处理获取map值对应include和exclude
        Map<String, List<String>> map = this.contentFieldHandle(contentFiled);
        try {
			Object handleResponseData = handleResponseData(body,map);
			return handleResponseData;
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
			return body;
		} catch (SecurityException e) {
			e.printStackTrace();
			return body;
		}
    }

    @Override
    public boolean supports(MethodParameter arg0, Class arg1) {
        return true;
    }

    public Map<String,List<String>> contentFieldHandle(String param){
    	Map<String,List<String>> result = new HashMap<String,List<String>>();
    	String[] split = StrUtil.split(param, ";");
    	for(String s : split) {
    		String[] split2 = StrUtil.split(s, "=");
    		String fields = split2[1];
    		List<String> filedList = StrUtil.split(fields, StrUtil.C_COMMA);
    		result.put(split2[0],filedList);
    	}
    	// include优先级高于exclude 去除exclude中间无用数值
    	if(ToolUtil.equals(result.size(), 2)) {
			/*List<String> includeFields = (List<String>) result.get(INCLUDE);
			List<String> excludeFields = (List<String>) result.get(EXCLUDE);
			// 交集
			List<String> intersection = includeFields.stream().filter(item -> excludeFields.contains(item)).collect(toList());
			excludeFields.removeAll(intersection);
			result.put(EXCLUDE, excludeFields);*/
    		
    		// include 和 exclude互斥 所以有include 就可以忽视 exclude
    		result.remove(EXCLUDE);
    	}
    	return result;
    }
    
    public Object handleResponseData(Object obj,Map<String,List<String>> map) throws NoSuchFieldException, SecurityException {
    	System.out.println("handleResponseData 处理中");
    	R r = (R)obj;
    	Object object = r.get("data");
    	if(object instanceof PageUtils) {
    		PageUtils data = (PageUtils)object;
    		List<?> list = data.getList();
    		Class c = list.get(0).getClass();
    		List<Object> result = new ArrayList<Object>();
    		for(Object param : list) {
    			Object res = filterFieldsJson(param,c,map);
    			result.add(res);
    		}
        	data.setList(result);
    	}else {
    		Class c = object.getClass();
    		Object res = filterFieldsJson(object,c,map);
    		r.put("data", res);
    	}
    	return obj;
    }
    
    
    /**
     * Description:过滤实体中的字段
     * @throws SecurityException 
     * @throws NoSuchFieldException 
     */
    public static Object filterFieldsJson(Object src, Class<?> clazz, Map<String,List<String>> map) throws NoSuchFieldException, SecurityException
    {
    	boolean isExclude =true;
    	ComplexPropertyPreFilter filter = new ComplexPropertyPreFilter();
    	Map<Class<?>, List<String>> filterMap = new HashMap<Class<?>,List<String>>();
    	filterMap.put(clazz, new ArrayList<String>());
       	List<String> fields = map.get(EXCLUDE);
    	if(ToolUtil.isEmpty(fields)) {
    		fields = map.get(INCLUDE);
    		isExclude = false; 
    	}
    	for(String field : fields) {
    		if(field.contains(StrUtil.DOT)) {
    			String[] split = StrUtil.split(field, StrUtil.DOT);
    			String attributeName = split[0];
    			Field declaredField = clazz.getDeclaredField(attributeName);
    			Class<?> attributeType = declaredField.getType();
    			List<String> list = filterMap.get(attributeType);
    			if(!isExclude) {
    				filterMap.get(clazz).add(split[0]);
    			}
    			if(ToolUtil.isEmpty(list)) {
    				List<String> s = new ArrayList<String>();
        			s.add(split[1]);
        			filterMap.put(attributeType, s);
    			}else {
    				list.add(split[1]);
    			}
    		}else {
    			filterMap.get(clazz).add(field);
    		}
    	}
    	
    	if(isExclude) {
    		filter.setExcludes(filterMap);
    	}else {
    		filter.setIncludes(filterMap);
    	}
        String jsonString = JSON.toJSONString(src, filter,SerializerFeature.WriteMapNullValue);
        return JSON.parseObject(jsonString);
    }
}
