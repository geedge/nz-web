package com.nis.modules.sys.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.service.SysService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.Charset;
import java.util.Map;

@RestController
@RequestMapping("/sys")
public class SysController {

    private static final Log log = Log.get();

    @Autowired
    private SysService sysService;

    @GetMapping("/appearance")
    public R queryAppearance() {
        Map<String, String> result = sysService.queryAppearance();
        return R.ok(result);
    }


    /**
     * 消息通知接口
     *
     * @param params
     * @return
     */
    @PostMapping("/component/message")
    public R processSystemMessage(@RequestBody Map<String, Object> params) {
        String topicName = Tool.MapUtil.getStr(params, "topic");
        Object messageObj = Tool.MapUtil.get(params, "message", Object.class, "");
        log.info("[processSystemMessage] [topicName: {}]", topicName);

        Constant.SysTopicNameEnum instance = Constant.SysTopicNameEnum.getInstanceByName(topicName);
        if (ObjectUtil.isNull(instance)) {
            log.warn("[processSystemMessage] [topicName is invalid] [topicName: {}]", topicName);
        } else {
            switch (instance) {
                case SYS_RESTORE -> {
                    String serverId = StrUtil.str(messageObj, Charset.forName("UTF-8"));
                    sysService.processSysRestore(serverId);
                }
                case SNMP_TRAP_PORT -> {
                }
                case DASHBOARD_REPORT -> {
                }
                case SYS_BACKUP -> {
                }
                case SYS_CONFIG_RELOAD -> {
                }
            }
        }
        return R.ok();
    }

}
