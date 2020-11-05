package com.nis.modules.metric.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.annotation.SysLog;
import com.nis.common.utils.*;
import com.nis.modules.alert.entity.AlertMessageEntity;
import com.nis.modules.alert.service.AlertMessageService;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.Idc;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.asset.service.IdcService;
import com.nis.modules.endpoint.service.EndpointService;
import com.nis.modules.metric.dto.NezhaMetrics;
import com.nis.modules.module.service.ModuleService;
import com.nis.modules.overview.dao.OverviewDao;
import com.nis.modules.project.service.ProjectService;
import com.nis.modules.promserver.entity.Promserver;
import com.nis.modules.promserver.service.PromserverService;
import com.nis.modules.sys.controller.AbstractController;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MetricsController extends AbstractController {

    @Autowired
    private NezhaMetrics nezhaMetrics;
    
    @Autowired
	private MeterRegistry meterRegistry;
    
    @Autowired
    private ProjectService projectService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private IdcService idcService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AlertMessageService alertMessageService;

    @Autowired
    private PromserverService promserverService;

    @Autowired
    private OverviewDao overviewDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(value = "/metrics", method = RequestMethod.GET)
    public ModelAndView counter1() {
        nezhaMetrics.bindTo(meterRegistry);
        ModelAndView mv = new ModelAndView();
        mv.setViewName("redirect:/prometheus");
        return mv;
    }

    /**
     * Nezha系统接口
     *
     * @return
     */
    @RequestMapping(value = "/system/info", method = RequestMethod.GET)
    @SysLog(operation = OperationEnum.QUERY, type = TypeEnum.SYSTEM)
    public R systemInfo() {
        Map resultMap = new HashMap();
        // 1. project总数
        resultMap.put("project", projectService.count());
        // 2. module总数
        resultMap.put("module", moduleService.count());
        // 3. endpoint总数
        resultMap.put("endpoint", endpointService.count());
        // 4. dc总数
        resultMap.put("dc", idcService.count());
        // 5. asset 在库总数
        resultMap.put("asset", assetService.count(new QueryWrapper<Asset>().eq("state", 1)));
        // 6. pending alert message 数量
        int alertCount = alertMessageService.count(new QueryWrapper<AlertMessageEntity>().eq("state", 1));
        resultMap.put("alert", alertCount);
        // 7.系统内所有global prometheus是否可用
        Map systemAlert = new HashMap();
        int count = promserverService.count(new QueryWrapper<Promserver>().eq("type", 1).eq("status", 1));
        String allGlobal = "unavailable";
        if (count > 0) {
            allGlobal = "available";
        }
        systemAlert.put("allGlobal", allGlobal);
        // 8. 各个dc下的per-datacenter是否可用
        List<String> idcNames = overviewDao.getIdcNameByPerPromeServer();
        List perDcList = new ArrayList();
        Map perDc = new HashMap();
        for (String name : idcNames) {
            perDc.put(name, "available");
        }

        List<Idc> idcList = idcService.list();
        if (CollectionUtils.isNotEmpty(idcNames) && idcNames.size() < idcList.size()) {
            for (Idc idc : idcList) {
                // 如果没有包含补充默认值 证明该dc下没有符合条件的per prometheus
                if (!idcNames.contains(idc.getName())) {
                    perDc.put(idc.getName(), "unavailable");
                }
            }
        }
        perDcList.add(perDc);
        systemAlert.put("perDc", perDcList);
        resultMap.put("systemAlert", systemAlert);
        return R.ok(resultMap);
    }

    @GetMapping("/healthy")
    public R healthy() {
        return R.ok();
    }

    /*private String redisHealthy() {
        String state = "";
        try {
            Object execute = redisTemplate.execute(new RedisCallback<String>() {
                @Override
                public String doInRedis(RedisConnection connection) throws DataAccessException {
                    return connection.ping();
                }
            });

            if ("PONG".equalsIgnoreCase(execute.toString())) {
                state = "UP";
            } else {
                state = "DOWN";
            }
        } catch (Exception e) {
            state = "DOWN";
            logger.error("检查 redis 状态出现问题, 错误信息是：" + e.getMessage(), e);
        } finally {
            return state;
        }
    }

    private String mysqlHealthy() {
        String state = "";
        try {
            String healthy = overviewDao.healthy();
            if ("1".equals(healthy)) {
                state = "UP";
            } else {
                state = "DOWN";
            }
        } catch (Exception e) {
            state = "DOWN";
            logger.error("检查 mysql 状态出现问题, 错误信息是：" + e.getMessage(), e);
        } finally {
            return state;
        }
    }*/
}