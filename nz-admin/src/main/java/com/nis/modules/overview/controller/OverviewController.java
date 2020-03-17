package com.nis.modules.overview.controller;

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
     * 1、全系统信息统计
     *
     * @return
     */
    /*@GetMapping("/byTypeStat")
    public R byTypeStat() {
        Map data = overviewService.byTypeStat();
        return R.ok(data);
    }*/


    /**
     * 2、系统采集组件的状态统计
     *
     * @return
     */
    /*@GetMapping("/scrapeModuleStat")
    public R scrapeModuleStat() {
        Map data = overviewService.scrapeModuleStat();
        return R.ok(data);
    }*/

    /**
     * 3.告警信息统计
     *
     * @return
     */
    /*@GetMapping("/alertStat")
    public R alertStat() {
        Map data = overviewService.alertStat();
        return R.ok(data);
    }*/

    /**
     * 4. 告警数量topN
     *
     * @param topN
     * @return
     */
   /* @GetMapping("/alertNumTopN")
    public R alertNumTopN(Integer topN) {
        ValidateUtils.is(topN).notNull(RCode.ALERTMSG_TOPN_ISNULL);

        List<Map> list = overviewService.alertNumTopN(topN);
        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }*/


    /**
     * 5、按告警规则维度统计告警信息的topN
     *
     * @param topN
     * @return
     */
   /* @GetMapping("/alertRuleTopN")
    public R alertRuleTopN(Integer topN) {
        ValidateUtils.is(topN).notNull(RCode.ALERTRULE_TOPN_ISNULL);

        List<Map> list = overviewService.alertRuleTopN(topN);
        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }*/


    /**
     * 6、未处理告警（按照告警级别，告警开始排序分页加载）
     *
     * @param pageNo
     * @param pageSize
     * @return
     */
    /*@GetMapping("/alertUnprocessed")
    public R alertUnprocessed(Integer pageNo, Integer pageSize) {
        List<Map> list = overviewService.alertUnprocessed(pageNo, pageSize);

        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }*/


    /**
     * 7、资产型号统计
     *
     * @param onlyInUse 是否只统计在库的资产  1 or 0
     * @return
     */
  /*  @GetMapping("/assetModelStat")
    public R assetModelStat(Integer onlyInUse) {
        if (onlyInUse == null) {
            onlyInUse = 1;
        }

        List<Map> list = overviewService.assetModelStat(onlyInUse);
        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }*/

    /**
     * 8、按DC维度统计asset信息
     *
     * @param onlyInUse 是否只统计在库的资产  1 or 0
     * @return
     */
    /*@GetMapping("/assetDCStat")
    public R assetDCStat(Integer onlyInUse) {
        if (onlyInUse == null) {
            onlyInUse = 1;
        }

        List<Map> list = overviewService.assetDCStat(onlyInUse);
        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }*/

    /**
     * 9、按DC维度统计prometheus server，endpoint
     *
     * @return
     */
    /*@GetMapping("/promDCStat")
    public R promDCStat() {
        List<Map> list = overviewService.promDCStat();
        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }*/

    /**
     * 10、告警规则统计
     *
     * @return
     */
    /*@GetMapping("/alertRuleStat")
    public R alertRuleStat() {
        Map map = overviewService.alertRuleStat();
        return R.ok(map);
    }*/

    /**
     * 11. 系统用户统计
     *
     * @return
     */
    /*@GetMapping("/sysUserStat")
    public R sysUserStat() {
        Map data = overviewService.sysUserStat();
        return R.ok(data);
    }
*/

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
            top = 10;
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
            top = 10;
        }

        List<Map> list = overviewService.alertStatByAssetTopN(top);

        Map data = new HashMap();
        data.put("list", list);
        return R.ok(data);
    }
}
