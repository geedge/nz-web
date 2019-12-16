package com.nis.modules.promserver.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverProxyService;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.promserver.utils.HttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class PromserverProxyServiceImpl implements PromserverProxyService {

    private Logger logger= LoggerFactory.getLogger(PromserverProxyServiceImpl.class);
    @Autowired
    private PromserverService promserverService;
    @Value("${spring.jackson.date-format}")
    private String dateFormate;

    @Override
    public JSONObject requestPromAPI(String url){
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
    @Override
    public String getPromServer(){

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

    @Override
    public String buildUrl(String baseUrl, Map<String, Object> params, Promserver promserver)  {
        StringBuilder sb=new StringBuilder("http://");
        String server="";
        if(promserver == null){
            server=getPromServer();
        }else{
            server=promserver.getHost()+":"+promserver.getPort();
        }

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
    @Override
    public Long formatTimeToUnixTimestemp(String time){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormate);
        try {
            Date date = simpleDateFormat.parse(time);
            return date.getTime()/1000;
        } catch (ParseException e) {
            throw new NZException(RCode.PROMSERVER_PARAM_TIME_INVALIDE);
        }
    }

    @Override
    public List<Map<String, Object>> formateTargetsData(JSONObject responseData) {
        Map<String,Object> data =(Map<String,Object>) responseData.get("data");
        List<Map<String,Object>> activeTargets = (List<Map<String,Object>>)data.get("activeTargets");

        for (Map<String,Object> activeTarget:activeTargets){
            Map<String,Object> discoveredLabels =(Map<String,Object>) activeTarget.get("discoveredLabels");

            String jobName=(String)discoveredLabels.get("job");

            Integer endpointId=Integer.parseInt(jobName.split("ed_")[1]);

            activeTarget.put("endpointId",endpointId);
        }

        return activeTargets;
    }


}
