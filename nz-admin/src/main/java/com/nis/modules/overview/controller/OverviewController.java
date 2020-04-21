package com.nis.modules.overview.controller;

import com.nis.common.utils.Constant;
import com.nis.common.utils.R;
import com.nis.modules.overview.service.OverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 全系统信息统计 controller
 *
 * @author shizhendong
 * @date 2020-02-04
 */
@RestController
@RequestMapping("/overview")
public class OverviewController {

    @Autowired
    private OverviewService overviewService;

    /**
     * asset统计信息
     *
     * @return
     */
    @GetMapping("/assetStat")
    public R assetStat() {
        Map assetStat = overviewService.getAssetStat();
        return R.ok(assetStat);
    }

    /**
     * project统计信息
     *
     * @return
     */
    @GetMapping("/projectStat")
    public R projectStat() {
        Map projectStat = overviewService.getProjectStat();
        return R.ok(projectStat);
    }

    /**
     * endpointStat统计信息
     *
     * @return
     */
    @GetMapping("/endpointStat")
    public R endpointStat() {
        Map endpointStat = overviewService.getEndpointStat();
        return R.ok(endpointStat);
    }


    /**
     * datacenter统计信息
     *
     * @return
     */
    @GetMapping("/datacenterStat")
    public R datacenterStat() {
        Map datacenterStat = overviewService.getDatacenterStat();
        return R.ok(datacenterStat);
    }

    /**
     * 告警规则统计信息
     *
     * @return
     */
    @GetMapping("/alertRuleStat")
    public R alertRuleStat() {
        Map alertRuleStat = overviewService.getAlertRuleStat();
        return R.ok(alertRuleStat);
    }

    /**
     * 按告警规则维度统计告警信息的topN
     *
     * @param top
     * @return
     */
    @GetMapping("/alertStatByRule")
    public R alertStatByRule(Integer top) {
        if (top == null) {
            top = (int) Constant.PAGESIZE;
        }

        List<Map> list = overviewService.alertRuleTopN(top);

        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }

    /**
     * 按asset维度统计告警信息的topN
     *
     * @param top
     * @return
     */
    @GetMapping("/alertStatByAsset")
    public R alertStatByAsset(Integer top) {
        if (top == null) {
            top = (int) Constant.PAGESIZE;
        }

        List<Map> list = overviewService.alertStatByAssetTopN(top);

        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }

    /**
     * module统计信息
     *
     * @return
     */
    @GetMapping("/moduleStat")
    public R moduleStat() {
        Map moduleStat = overviewService.getModuleStat();
        return R.ok(moduleStat);
    }

    /**
     * 告警消息统计信息
     *
     * @return
     */
    @GetMapping("/alertMessageStat")
    public R alertMessageStat() {
        Map data = overviewService.alertMessageStat();
        return R.ok(data);
    }

    /**
     * 按module维度统计告警信息的topN
     *
     * @param top
     * @return
     */
    @GetMapping("/alertStatByModule")
    public R alertStatByModule(Integer top) {
        if (top == null) {
            top = (int) Constant.PAGESIZE;
        }

        List<Map> list = overviewService.alertStatByModuleTopN(top);

        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }
}
