package com.nis.modules.agent.util;

import cn.hutool.log.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentUtil  {

    private static Log log = Log.get();

    /**
     * 加载 cmdTmpl 字符串 并转为 map
     * @param configTmplStr
     * @return
     */
    public static Map<String,String> loadServiceConfigStr(String configTmplStr){
        //匹配有参类型  --[\w\.-]+?\s 可匹配无参类型
        Pattern pattern = Pattern.compile("--([\\w\\.-]+?)='?(.+?)'?\\s+?");
        Matcher matcher = pattern.matcher(configTmplStr);

        Map<String, String> result = new HashMap<>(16);
        while(matcher.find()){
            result.put(matcher.group(1).trim(),matcher.group(2).trim());
        }
        return result;
    }
}
