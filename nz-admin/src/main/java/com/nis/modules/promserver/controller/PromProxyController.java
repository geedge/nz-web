package com.nis.modules.promserver.controller;

import com.alibaba.fastjson.JSONObject;
import com.nis.modules.promserver.entity.PromApi;
import com.nis.modules.promserver.service.PromserverProxyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;

@Controller
@ConfigurationProperties(prefix = "nezha")
public class PromProxyController {

    private Logger logger= LoggerFactory.getLogger(PromProxyController.class);

    @Autowired
    private PromserverProxyService promserverProxyService;

    @Autowired
    private PromApi promApi;

    @RequestMapping(value = "/prom/**",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public JSONObject promProxy(HttpServletRequest request,@RequestParam Map<String,Object> params){
        // /nz-admin/prom/api/v1/query_range
        String requestURI = request.getRequestURI();
        String api=requestURI.split("/prom")[1];
        Set<Map.Entry<String, String>> entries = promApi.getPromApis().entrySet();
        boolean findFlag=false;
        JSONObject result=null;
        for (Map.Entry<String,String> entry:entries){
            String key=entry.getKey();
            String value=entry.getValue();
            if(api.matches(value)){
                findFlag=true;
                result=promserverProxyService.excuteMethod(key,api,params);
                break;
            }
        }

        if(!findFlag){
            result=promserverProxyService.defaultExcute(api,params);
        }
        return result;
    }


}
