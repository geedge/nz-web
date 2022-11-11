package com.nis.modules.panel.service.impl;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.ResponseUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.LokiApiService;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.panel.service.VisualService;
import com.nis.modules.sys.entity.Timezone;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.TimezoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Service("visualService")
public class VisualServiceImpl implements VisualService {

	private Log log = Log.get();
    @Value("${connect.timeout:30000}")
    private Integer connectTimeout;
    
    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private PromApiService promApiService;

    @Autowired
    private LokiApiService lokiApiService;

    @Autowired
    private TimezoneService timezoneService;

    @Override
    public void exploreSnapshot(HttpServletResponse response, String format, Integer type, Long start, Long end, String language, Integer unit, String limit, String direction, String[] expressions) throws IOException {
        // 计算步长
        String step = this.getTimeStep(start, end);
        if(!StrUtil.equals(direction, "forward")){
            direction = "backward";
        }

        List<CompletableFuture<JSONObject>> futureList = new ArrayList<>();
        for (int i = 0; i < expressions.length; i++) {
            String expression = URLUtil.decode(expressions[i]);
            String finalDirection = direction;
            futureList.add(CompletableFuture.supplyAsync(() -> {
                JSONObject queryData = null;
                if(type == 1){
                    queryData = promApiService.query_range(expression, StrUtil.toString(start), StrUtil.toString(end), step, null, null);
                } else if (type == 2) {
                    queryData = lokiApiService.query_range(expression, StrUtil.toString(start), StrUtil.toString(end), StrUtil.emptyToDefault(limit, "100"), null, null, finalDirection, "1", null);
                }
                queryData.put("expression", expression);
                return queryData;
            }));
        }
        List<JSONObject> dataList = new ArrayList();
        try{
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).get(connectTimeout, TimeUnit.MILLISECONDS);
            futureList.forEach(future -> {
                JSONObject result = future.getNow(null);
                if (Tool.MapUtil.isNotEmpty(result)) {
                    dataList.add(result);
                }
            });
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e);
        }
        // 封装返回结果
        Map<String, Object> resultMap = this.snapshotResultMap(start, end, language);
        resultMap.put("type", type);
        resultMap.put("unit", unit);
        resultMap.put("data", dataList);

        String snapshotTemplate = this.replaceSnapshotTemplate(resultMap);
        // 目前下载名称由前端控制
        // String downFileName = String.format("%s-%s.%s", "explore", System.currentTimeMillis(), StrUtil.emptyToDefault(format, "html"));
        ResponseUtil.downloadFile(response, "exploreSnapshot.html", snapshotTemplate.getBytes());
    }

    /**
     * 模板基本返回参数
     */
    @Override
    public Map<String, Object> snapshotResultMap(Long start, Long end, String language){
        Map<String, Object> resultMap = Tool.MapUtil.newHashMap();
        resultMap.put("language", language);
        resultMap.put("start", start);
        resultMap.put("end", end);
        // 查询系统时区信息
        String timezone = sysConfigService.getValue("timezone");
        Timezone timezoneEntity = timezoneService.getOne(new LambdaUpdateWrapper<Timezone>().eq(Timezone::getName, timezone));
        resultMap.put("offset", timezoneEntity.getOffset());
        resultMap.put("timezone", timezone);
        return resultMap;
    }

    /**
     * 读取快照模板，替换数据
     */
    @Override
    public String replaceSnapshotTemplate(Map<String, Object> resultMap){
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            String path = sysConfigService.getValue("snapshot_template_path");
            reader = new BufferedReader(new FileReader(Tool.StrUtil.concat(true, path, File.separator, "snapshot_template.html")));
            String line;
            while ((line = reader.readLine()) != null) {
                if (Tool.StrUtil.startWith(line, "<script>window.dataJson"))
                    line = "<script>window.dataJson=" + JSONUtil.toJsonStr(resultMap) + "</script>";
                sbf.append(line + "\n");
            }
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (reader != null) IoUtil.close(reader);
        }
        return sbf.toString();
    }

    /**
     * 计算Dashboards step
     */
    @Override
    public String getTimeStep(Long start, Long end){
        Long timeDiff = (end - start) / (24 * 60 * 60);
        if(timeDiff < 1){
            return "15s";
        }else if(timeDiff < 7){
            return "5m";
        }else if(timeDiff < 30){
            return "10m";
        }else{
            return "30m";
        }
    }
}
