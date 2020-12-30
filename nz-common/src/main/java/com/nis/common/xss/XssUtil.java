package com.nis.common.xss;

import org.apache.commons.lang.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XssUtil {
    public static String filterString(String str){
        if(StringUtils.isNotBlank(str))
            return HtmlUtils.htmlEscape(str,"UTF-8");
        return str;
    }

    public static Map<String,Object> filterParameters(Map<String,Object> params){
        if(params != null){
            for (Map.Entry<String,Object> entry:params.entrySet()){
                Object value = entry.getValue();
                if(value instanceof String){
                    String val = filterString((String)value);
                    params.put(entry.getKey(),val);
                }
            }
            return params;
        }
        return null;
    }

    public static String stripXSS(String str){
        str=filterString(str);
        String regex="[\\r\\n\\\\r\\\\n]";
        Pattern pattern = Pattern.compile(regex,Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(str);
        StringBuffer sb=new StringBuffer();
        while (matcher.find()){
            matcher.appendReplacement(sb,"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

}
