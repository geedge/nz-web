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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class MetricsController extends AbstractController {

    @Value("${spring.datasource.druid.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.druid.url}")
    private String dbUrl;

    @Value("${spring.datasource.druid.username}")
    private String dbUsername;

    @Value("${spring.datasource.druid.password}")
    private String dbPassword;

    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private Integer redisPort;

    @Value("${spring.redis.timeout}")
    private String redisTimeout;

    @Value("${spring.redis.password}")
    private String redisPassword;

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
        Map healthyMap = new HashMap(4);

        healthyMap.put("web", "UP");

        String mysqlState = mysqlHealthy();
        String redisState = redisHealthy();

        healthyMap.put("mysql", mysqlState);
        healthyMap.put("redis", redisState);

        if (("DOWN".equals(mysqlState) || "DOWN".equals(redisState))) {
            R r = new R();
            r.put("code", RCode.ERROR.getCode());
            r.put("msg", RCode.ERROR.getMsg());
            r.put("time", DateUtil.getUTCTimeByConfigTimeZone());
            r.put("data", healthyMap);
            return r;
        }

        return R.ok(healthyMap);
    }

    private String redisHealthy() {
        logger.info(String.format("检查 redis 健康状态，地址是：%s,端口是：%s,密码是:%s,登录超时为：%s", redisHost, redisPort, redisPassword, redisTimeout));
        String state = "";
        Jedis jedis = null;
        try {
            Integer timeout;
            if (StringUtils.isEmpty(redisTimeout)) {
                timeout = 6000;
            } else {
                Matcher matcher = Pattern.compile("[^0-9]").matcher(redisTimeout);
                timeout = Integer.valueOf(matcher.replaceAll("").trim());
            }

            jedis = new Jedis(redisHost, redisPort, timeout);
            if (StringUtils.isNotEmpty(redisPassword)) {
                jedis.auth(redisPassword);
            }

            String ping = jedis.ping();
            if (ping.equalsIgnoreCase("PONG")) {
                state = "UP";
            } else {
                state = "DOWN";
            }
        } catch (Exception e) {
            logger.error(String.format("检查 redis 状态出现问题，地址是：%s,端口是：%s,密码是:%s,登录超时为：%s,错误信息是:%s", redisHost, redisPort, redisPassword, redisTimeout, e.getMessage()), e);
            state = "DOWN";
        } finally {
            if (jedis != null) jedis.close();
            return state;
        }
    }

    private String mysqlHealthy() {
        logger.info(String.format("检查 mysql 健康状态，加载类是：%s，地址是：%s,用户名是：%s,密码是:%s", driverClassName, dbUrl, dbUsername, dbPassword));
        String state = "";
        Connection connection = null;
        Statement statement = null;
        ResultSet rs = null;
        try {
            connection = getConnection(driverClassName, dbUrl, dbUsername, dbPassword);
            if (connection == null && !connection.isClosed()) {
                state = "DOWN";
            } else {
                statement = connection.createStatement();
                String sql = "SELECT 1;";
                rs = statement.executeQuery(sql);
                String result = "";
                while (rs.next()) {
                    result += rs.getString("1");
                }
                if ("1".equals(result)) {
                    state = "UP";
                } else {
                    state = "DOWN";
                }
            }
        } catch (Exception e) {
            logger.error(String.format("检查 mysql 状态出现问题，加载类是：%s，地址是：%s,用户名是：%s,密码是:%s,错误信息是：%s", driverClassName, dbUrl, dbUsername, dbPassword, e.getMessage()), e);
            state = "DOWN";
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return state;
        }
    }

    private static Connection getConnection(String driverClassName, String url, String username, String pwd) {
        try {
            Class.forName(driverClassName);
            Connection conn = DriverManager.getConnection(url, username, pwd);
            return conn;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}