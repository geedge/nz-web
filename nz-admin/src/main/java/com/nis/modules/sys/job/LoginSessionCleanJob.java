package com.nis.modules.sys.job;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.modules.sys.entity.SysApiKey;
import com.nis.modules.sys.entity.SysLoginSession;
import com.nis.modules.sys.service.SysApiKeyService;
import com.nis.modules.sys.service.SysConfigService;
import com.nis.modules.sys.service.SysLoginSessionService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class LoginSessionCleanJob extends QuartzJobBean {

    private Log log = Log.get();

    @Autowired
    private SysLoginSessionService sessionService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SysConfigService configService;

    @Autowired
    private SysApiKeyService sysApiKeyService;


    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        Thread.currentThread().setName("loginSessionCleanJob");
        // 检查 haMode
        String serverId = redisTemplate.opsForValue().get(Constant.SYS_HA_LOCK);
        if (!StrUtil.equals(serverId, Constant.SERVER_ID)) {
            return;
        }

        //获取 session 过期时间
        String sessionTimeOut = StrUtil.emptyToDefault(configService.getValue(Constant.SYSCONFIG_SESSION_TIMEOUT_KEY), "30");

        //获取所有的 apikey
        List<SysApiKey> apiKeyList = sysApiKeyService.list();
        //获取所有 apikey token
        List<String> allApiKeyTokenList = apiKeyList.stream().map(apiKey -> apiKey.getToken()).collect(Collectors.toList());

        //获取所有的已过期的 api key
        Date now = new Date();
        String time = DateUtil.format(now, "yyyy-MM-dd hh:mm:ss");
        List<SysApiKey> expiredApiKeyList = sysApiKeyService.list(new LambdaQueryWrapper<SysApiKey>()
                .lt(SysApiKey::getExpireAt, time)
                .and(wapper -> wapper.isNotNull(SysApiKey::getExpireAt)));
        List<String> expiredApiKeyTokenList = expiredApiKeyList.stream().map(apiKey -> apiKey.getToken()).collect(Collectors.toList());
        sessionService.removeBatchByIds(expiredApiKeyTokenList);


        //获取所有 session
        List<SysLoginSession> sessionList = sessionService.list();
        //过滤 apikey token
        sessionList = sessionList.stream().filter(session -> !allApiKeyTokenList.contains(session.getSessionId())).toList();
        for (SysLoginSession session : sessionList) {
            //删除过期的 session
            if (System.currentTimeMillis() > (session.getLasetActiveTs() + Long.valueOf(sessionTimeOut) * DateUnit.MINUTE.getMillis())) {
                sessionService.removeById(session.getSessionId());
            }
        }
    }
}
