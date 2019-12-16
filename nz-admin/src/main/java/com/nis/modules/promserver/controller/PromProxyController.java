package com.nis.modules.promserver.controller;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.MemeryCacheData;
import com.nis.common.utils.R;
import com.nis.common.utils.RCode;
import com.nis.modules.promserver.entity.PromApi;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverProxyService;
import com.nis.modules.promserver.service.PromserverService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;

@Controller
@ConfigurationProperties(prefix = "nezha")
@RequestMapping("/prom")
public class PromProxyController {

    private Logger logger= LoggerFactory.getLogger(PromProxyController.class);

    @Autowired
    private PromserverService promserverService;
    @Autowired
    private PromserverProxyService promserverProxyService;

    @Autowired
    private PromApi promApi;
    @GetMapping(value="/api/v1/targets")
    @ResponseBody
    public R targets(@RequestParam Map<String,Object> params){
        List<Object> allProms = MemeryCacheData.memeryCache.get(MemeryCacheData.PROM_SERVER_CACHE);
        List<Promserver> promservers = new ArrayList<Promserver>();
        if(params.get("idcId") != null){
            for(Object prom:allProms){
                Promserver promserver=(Promserver)prom;
                if(promserver.getType().intValue()== 2){
                    if(promserver.getIdcId().intValue() == Integer.parseInt((String) params.get("idcId"))){
                        promservers.add(promserver);
                    }
                }


            }
        }else{
            for(Object prom:allProms){
                Promserver promserver=(Promserver)prom;
                if(promserver.getType().intValue()== 2){
                    promservers.add(promserver);
                }
                promservers.add(promserver);
            }
        }
        List<Map<String,Object>> list=new ArrayList<>();
        HashMap<String, Object> result = new HashMap<>();
        if(promservers.size()>0){
            CountDownLatch latch=new CountDownLatch(promservers.size());

            ExecutorService executorService = Executors.newFixedThreadPool(10);

            BlockingQueue<Map<String,Object>> queue=new LinkedBlockingQueue<>();

            for(int i=0;i<promservers.size();i++){

                Promserver promserver=promservers.get(i);
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        String url=promserverProxyService.buildUrl("/api/v1/targets",null,promserver);
                        try{
                            JSONObject responseData = promserverProxyService.requestPromAPI(url);
                            if (responseData != null){
                                List<Map<String, Object>> activeTargets = promserverProxyService.formateTargetsData(responseData);
                                queue.addAll(activeTargets);
                            }
                        }catch (Exception e ){
                            e.printStackTrace();
                        }finally {
                            latch.countDown();
                        }
                    }
                });

            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            queue.drainTo(list);



        }
        result.put("list",list);
        return R.ok(result);
    }
    @RequestMapping(value = "/**",method = {RequestMethod.GET,RequestMethod.POST})
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
            params.put("time",promserverProxyService.formatTimeToUnixTimestemp((String)params.get("time")));
        }

        String url=promserverProxyService.buildUrl(api,params,null);

        return promserverProxyService.requestPromAPI(url);
    }

    // /api/v1/query_range
    public JSONObject queryRange(String api,Map<String,Object> params){
        //exmple: api/v1/query_range?query=up&start=2015-07-01T20:10:30.781Z&end=2015-07-01T20:11:00.781Z&step=15s
        if(!params.containsKey("query")||StringUtils.isBlank((String)params.get("query"))){
            throw new NZException(RCode.PROMSERVER_PARAM_QUERY_ISNULL);
        }

        if(params.containsKey("start")&&StringUtils.isNotBlank((String)params.get("start"))){
            params.put("start",promserverProxyService.formatTimeToUnixTimestemp((String)params.get("start")));
        }else{
            throw new NZException(RCode.PROMSERVER_PARAM_START_ISNULL);
        }

        if(params.containsKey("end")&&StringUtils.isNotBlank((String)params.get("end"))){
            params.put("end",promserverProxyService.formatTimeToUnixTimestemp((String)params.get("end")));
        }else{
            throw new NZException(RCode.PROMSERVER_PARAM_END_ISNULL);
        }



        if(!params.containsKey("step")){
            throw new NZException(RCode.PROMSERVER_PARAM_STEP_ISNULL);
        }

        String url=promserverProxyService.buildUrl(api,params,null);
        return promserverProxyService.requestPromAPI(url);
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
            params.put("start",promserverProxyService.formatTimeToUnixTimestemp((String)params.get("start")));
        }

        if(params.containsKey("end")&&StringUtils.isNotBlank((String)params.get("end"))){
            params.put("end",promserverProxyService.formatTimeToUnixTimestemp((String)params.get("end")));
        }
        String url=promserverProxyService.buildUrl(api,params,null);
        return promserverProxyService.requestPromAPI(url);
    }

    // /api/v1/labels
    public JSONObject labels(String api,Map<String,Object> params){
        return  defaultExcute(api,params);
    }

    // /api/v1/label/<label_name>/values
    public JSONObject label(String api,Map<String,Object> params){
        return defaultExcute(api,params);
    }

    /**
     * 反射执行对应方法
     * */
    public JSONObject excuteMethod(String methodName, String api, Map<String, Object> params){
        Class clazz= PromProxyController.class;
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
            e.printStackTrace();
        }
        return result;
    }
    /**
     * 没有配置时 走默认方法
     * */
    public JSONObject defaultExcute(String api, Map<String, Object> params){
        String url=promserverProxyService.buildUrl(api,params,null);

        return promserverProxyService.requestPromAPI(url);
    }

}
