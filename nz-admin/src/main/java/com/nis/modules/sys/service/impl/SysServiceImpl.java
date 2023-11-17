package com.nis.modules.sys.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.utils.Constant;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.dao.SysDao;
import com.nis.modules.sys.entity.SysComponent;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysComponentService;
import com.nis.modules.sys.service.SysService;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SysServiceImpl extends ServiceImpl<SysDao, SysConfigEntity> implements SysService {

    private static final Log log = Log.get();

    public static final String[] BASIC_KEYS = {"system_name", "theme", "language", "date_format", "system_bgImg", "system_favicon", "system_logo", "timezone", "copyright"};


    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SysComponentService sysComponentService;

    @Override
    public Map<String, String> queryAppearance() {

        List<String> keys = Arrays.asList(BASIC_KEYS);
        List<SysConfigEntity> sysConfigList = this.list();

        Map<String, String> sysConfigMap = sysConfigList.stream()
                .filter(config -> keys.contains(config.getParamKey()))
                .collect(Collectors.toMap(SysConfigEntity::getParamKey, SysConfigEntity::getParamValue));

        String copyright = StrUtil.nullToDefault(sysConfigMap.get("copyright"), "0");
        sysConfigMap.put("copyright", copyright);
        sysConfigMap.put("dateFormat", sysConfigMap.remove("date_format"));

        return sysConfigMap;
    }

    @Override
    public void notifySysMessage(String componentName, String topic, Object message) {
        log.info("[notifySysMessage] [begin] [topic: {}]", topic);
        StopWatch sw = new StopWatch();
        sw.start();

        // requestBody
        Map<Object, Object> requestBody = Tool.MapUtil.builder()
                .put("topic", topic)
                .put("message", message)
                .build();

        // notify message
        List<SysComponent> sysComponentList = sysComponentService.list(new LambdaQueryWrapper<SysComponent>().eq(SysComponent::getComponent, componentName));
        sysComponentList = sysComponentList.stream()
                .sorted(Comparator.comparing(SysComponent::getServerid, Comparator.comparing(serverid -> StrUtil.equals(Constant.SERVER_ID, serverid))))
                .toList();

        for (SysComponent component : sysComponentList) {
            String host = component.getProtocol() + "://" + component.getHost() + ":" + component.getPort();
            String url = String.format("%s%s", host, "/sys/component/message");
            try {
                ResponseEntity<Map> entity = restTemplate.postForEntity(url, requestBody, Map.class);
                HttpStatusCode statusCode = entity.getStatusCode();
                log.info("[notifySysMessage] [url: {}] [responseStatus: {}] [result: {}]", url, statusCode.value(), Tool.JSONUtil.toJsonStr(entity));
            } catch (Exception e) {
                log.error(e, "[notifySysMessage] [error] [url: {}] [message: {}]", url, message);
            }
        }

        sw.stop();
        log.info("[notifySysMessage] [finshed] [Run time: {}]", sw.toString());
    }

    @Override
    public void processSysRestore(String serverId) {
        log.info("[processSysRestore] [begin] [serverId: {}]", serverId);
        SysComponent component = sysComponentService.getOne(new LambdaQueryWrapper<SysComponent>()
                .eq(SysComponent::getComponent, Constant.NZ_WEB_COMPONENT_NAME)
                .eq(SysComponent::getServerid, serverId));
        log.info("[processSysRestore] [check serverId is validate] [result: {}]", ObjectUtil.isNotNull(component));
        if (ObjectUtil.isNotNull(component)) {
            CompletableFuture.runAsync(() -> {
                try {
                    // restart nz-web
                    Tool.ThreadUtil.sleep(5, TimeUnit.SECONDS);
                    Tool.RuntimeUtil.exec("systemctl restart nz-web.service");
                } catch (Exception e) {
                    log.error(e, "[processSysRestore] [perform restart error]");
                }
            }).getNow(null);
        } else {
            log.warn("[processSysRestore] [serverId is invalid,not found web component] [serverId: {}]", serverId);
        }
        log.info("[processSysRestore] [finshed]");
    }
}
