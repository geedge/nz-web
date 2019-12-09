package com.nis.modules.promserver.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.entity.PromApi;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.utils.HttpClientUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@ConfigurationProperties(prefix = "nezha")
public class PromProxyController {

    private Logger logger= LoggerFactory.getLogger(PromProxyController.class);

    @Autowired
    private PromserverService promserverService;
    @Value("${spring.jackson.date-format}")
    private String dateFormate;
    @Autowired
    private PromApi promApi;

    @GetMapping("/prom/**")
    @ResponseBody
    public JSONObject promProxy(HttpServletRequest request,@RequestParam Map<String,Object> params){
        // /nz-admin/prom/api/v1/query_range
        String requestURI = request.getRequestURI();
        String api=requestURI.split("/nz-admin/prom")[1];
        Set<Map.Entry<String, String>> entries = promApi.getPromApis().entrySet();
        boolean findFlag=false;
        JSONObject result=null;
        for (Map.Entry<String,String> entry:entries){
            String key=entry.getKey();
            String value=entry.getValue();
            if(api.matches(value)){
                findFlag=true;
                result=excuteMethod(key,api,params);
                break;
            }
        }

        if(!findFlag){
            result=defaultExcute(api,params);
        }
        return result;
    }

    // /api/v1/query
    public JSONObject query( String api,Map<String,Object> params){
        //exmple:/api/v1/query?query=up&time=2015-07-01T20:10:51.781Z
        if(!params.containsKey("query")||StringUtils.isBlank((String)params.get("query"))){
            throw new NZException(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
        }
        //time 为非必填项，但格式只能为 rfc3339 | unix_timestamp
        if(params.containsKey("time")&&StringUtils.isNotBlank((String)params.get("time"))){
            params.put("time",formatTimeToUnixTimestemp((String)params.get("time")));
        }

        String url=buildUrl(api,params);

        return requestPromAPI(url);
    }

    // /api/v1/query_range
    public JSONObject queryRange(String api,Map<String,Object> params){
        //exmple: api/v1/query_range?query=up&start=2015-07-01T20:10:30.781Z&end=2015-07-01T20:11:00.781Z&step=15s
        if(!params.containsKey("query")||StringUtils.isBlank((String)params.get("query"))){
            throw new NZException(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
        }

        if(params.containsKey("start")&&StringUtils.isNotBlank((String)params.get("start"))){
            params.put("start",formatTimeToUnixTimestemp((String)params.get("start")));
        }else{
            throw new NZException(RCode.PROMSERVER_PARAM_START_ISNULL);
        }

        if(params.containsKey("end")&&StringUtils.isNotBlank((String)params.get("end"))){
            params.put("end",formatTimeToUnixTimestemp((String)params.get("end")));
        }else{
            throw new NZException(RCode.PROMSERVER_PARAM_END_ISNULL);
        }



        if(!params.containsKey("step")){
            throw new NZException(RCode.PROMSERVER_PARAM_STEP_ISNULL);
        }

        String url=buildUrl(api,params);
        return requestPromAPI(url);
    }

    // /api/v1/series
    public JSONObject series(String api, Map<String,Object> params){

        if(!params.containsKey("match[]")||StringUtils.isBlank((String)params.get("match[]"))){
            throw new NZException(RCode.PROMSERVER_PARAM_MATCH_ISNULL);
        }else{
            String matchsParam=(String)params.get("match[]");
            String[] matchs=matchsParam.split(",");

            params.put("match[]",Arrays.asList(matchs));
        }


        if(params.containsKey("start")&&StringUtils.isNotBlank((String)params.get("start"))){
            params.put("start",formatTimeToUnixTimestemp((String)params.get("start")));
        }

        if(params.containsKey("end")&&StringUtils.isNotBlank((String)params.get("end"))){
            params.put("end",formatTimeToUnixTimestemp((String)params.get("end")));
        }
        String url=buildUrl(api,params);
        return requestPromAPI(url);
    }

    // /api/v1/labels
    public JSONObject labels(String api,Map<String,Object> params){
        return  defaultExcute(api,params);
    }

    // /api/v1/label/<label_name>/values
    public JSONObject label(String api,Map<String,Object> params){
        return defaultExcute(api,params);
    }

    private JSONObject requestPromAPI(String url){
        JSONObject jsonObject=null;
        try{
            jsonObject = HttpClientUtils.httpGet(url);
            logger.info("request Prometheus Server query api with request:[{}]",url);

        }catch (Exception e){
            logger.error("request Prometheus Server query api with error {}",e);
            throw new NZException(RCode.PROMSERVER_REQUEST_FAILD);
        }
        return jsonObject;
    }

    /**
     * 随机获取数据库中配置的promServer 中的一个
     * */
    private String getPromServer(){

        QueryWrapper<Promserver> psQueryWrapper = new QueryWrapper<>();
        //服务器类型为center
        psQueryWrapper.eq("type",1);
        List<Promserver> list = promserverService.list(psQueryWrapper);

        if(list != null && list.size() > 0){
            int randomPos=new Random().nextInt(list.size());
            Promserver ramdomServer=list.get(randomPos);
            return ramdomServer.getHost()+":"+ramdomServer.getPort();
        }else{
            throw new NZException(RCode.PROMSERVER_HOST_ISNULL);
        }

    }


    private String buildUrl(String baseUrl,Map<String,Object> params)  {
        StringBuilder sb=new StringBuilder("http://");
        String server=getPromServer();
        sb.append(server); //ip+port
        sb.append(baseUrl); //url
        if(params!=null&&params.size()>0){
            sb.append("?");
            Set<Map.Entry<String, Object>> entries = params.entrySet();
            boolean isArray=false;
            for (Map.Entry<String,Object> entry:entries){
                String key=entry.getKey();
                Object value = entry.getValue();
                String parseValue="";
                if(value instanceof String){
                    parseValue=(String)value;
                    isArray=false;
                }

                if(value instanceof Long){
                    parseValue=(Long)value+"";
                    isArray=false;
                }

                if(value instanceof List){
                    String temp="";
                    for (String param:(List<String>)value){
                        try {
                            temp+=(URLEncoder.encode(key,"UTF-8")+"="+ URLEncoder.encode(param,"UTF-8")+"&");
                        }catch (Exception e){
                            logger.warn("urlencode faild");
                        }
                    }

                    parseValue=temp;
                    isArray=true;
                }
                if(isArray){
                    sb.append(parseValue);
                }else{
                    try {
                        sb.append((URLEncoder.encode(key,"UTF-8")+"="+parseValue+"&"));
                    }catch (Exception e){
                        logger.warn("urlencode faild");
                    }

                }

            }
            return sb.substring(0,sb.length()-1);
        }
        return sb.toString();
    }

    /**
     * 将时间字符串转为unix 时间戳
     * */
    private Long formatTimeToUnixTimestemp(String time){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormate);
        try {
            Date date = simpleDateFormat.parse(time);
            return date.getTime()/1000;
        } catch (ParseException e) {
            throw new NZException(RCode.PROMSERVER_PARAM_TIME_INVALIDE);
        }
    }
    /**
     * 反射执行对应方法
     * */
    private JSONObject excuteMethod(String methodName,String api,Map<String,Object> params){
        Class clazz=PromProxyController.class;
        JSONObject result=null;
        try {
            Method method = clazz.getMethod(methodName, String.class, Map.class);
            if(method != null){
                result =(JSONObject) method.invoke(this, api, params);

            }else{
                result= defaultExcute(api,params);
            }

        }catch (Exception e){
            result= defaultExcute(api,params);
        }
        return result;
    }
    /**
     * 没有配置时 走默认方法
     * */
    private JSONObject defaultExcute(String api,Map<String,Object> params){
        String url=buildUrl(api,params);

        return requestPromAPI(url);
    }
}
