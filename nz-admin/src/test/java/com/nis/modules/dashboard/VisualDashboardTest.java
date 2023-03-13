package com.nis.modules.dashboard;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.dashboard.entity.VisualDashboard;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class VisualDashboardTest extends BaseTest<VisualDashboard> {

    @Override
    protected String jsonIndex() {
        return "dashboard/VisualDashboardTestParameter.json";
    }

    /**
     * GET /visual/dashboard/{id} SUCCESS
     */
    @Test
    public void queryVisualDashboard_id() throws Exception {
        MvcResult result = UTUtil.performGet("/visual/dashboard/1", null);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);

        Map<String, Object> actualResult = JSONObject.parseObject(result.getResponse().getContentAsString(), Map.class);
        Object projectInfo = actualResult.get("data");
        Assert.assertNotNull(projectInfo);
    }

    /**
     * GET /visual/dashboard SUCCESS
     */
    @Test
    public void queryVisualDashboard_success() throws Exception {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/visual/dashboard", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);

        Map<String, Object> actualResult = JSONObject.parseObject(result.getResponse().getContentAsString(), Map.class);
        Object listInfo = actualResult.get("data");
        Assert.assertNotNull(listInfo);
    }

    /**
     * POST /visual/dashboard SUCCESS
     */
    @Test
    public void addVisualDashboard_success(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * POST /visual/dashboard DASHBOARD_NAME_ISNULL
     */
    @Test
    public void addVisualDashboard_nameIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_NAME_ISNULL);
    }

    /**
     * POST /visual/dashboard DASHBOARD_NAME_FORMAT_ERROR
     */
    @Test
    public void addVisualDashboard_nameFormatError(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_NAME_FORMAT_ERROR);
    }

    /**
     * POST /visual/dashboard DASHBOARD_NAME_DUPLICATE
     */
    @Test
    public void addVisualDashboard_nameDuplicate(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_NAME_DUPLICATE);
    }

    /**
     * POST /visual/dashboard DASHBOARD_TYPE_ISNULL
     */
    @Test
    public void addVisualDashboard_typeIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_TYPE_ISNULL);
    }

    /**
     * POST /visual/dashboard DASHBOARD_TYPE_INVALIDE
     */
    @Test
    public void addVisualDashboard_typeInvalide(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_TYPE_INVALIDE);
    }

    /**
     * POST /visual/dashboard DASHBOARD_NOT_EXIST
     */
    @Test
    public void addVisualDashboard_notExist(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_NOT_EXIST);
    }

    /**
     * POST /visual/dashboard DASHBOARD_NAME_TOO_LONG
     */
    @Test
    public void addVisualDashboard_error(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.CONTENT_MORETHAN_MAXLENGTH);
    }

    /**
     * PUT /visual/dashboard SUCCESS
     */
    @Test
    public void updateVisualDashboard_success(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPut("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * PUT /visual/dashboard DASHBOARD_ID_ISNULL
     */
    @Test
    public void updateVisualDashboard_idIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPut("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_ID_ISNULL);
    }

    /**
     * PUT /visual/dashboard DASHBOARD_NAME_TOO_LONG
     */
    @Test
    public void updateVisualDashboard_error(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPut("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.CONTENT_MORETHAN_MAXLENGTH);
    }

    /**
     * DELETE /visual/dashboard SUCCESS
     */
    @Test
    public void deleteVisualDashboard_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/visual/dashboard", map);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * DELETE /visual/dashboard DASHBOARD_ID_ISNULL
     */
    @Test
    public void deleteVisualDashboard_idIsnull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/visual/dashboard", map);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_ID_ISNULL);
    }

    /**
     * POST /duplicate/{id} SUCCESS
     */
    @Test
    public void duplicate_success(){
        MvcResult result = UTUtil.performPost("/visual/dashboard/duplicate/4", null);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * POST /duplicate/{id} DASHBOARD_NOT_EXIST
     */
    @Test
    public void duplicate_chartDashboardIsnull(){
        MvcResult result = UTUtil.performPost("/visual/dashboard/duplicate/8", null);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_NOT_EXIST);
    }

    /**
     * POST /duplicate/{id} DASHBOARD_NAME_TOO_LONG
     */
    @Test
    public void duplicate_dashboardNameTooLong(){
        MvcResult result = UTUtil.performPost("/visual/dashboard/duplicate/1", null);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_NAME_TOO_LONG);
    }
    /** ------------------- param 参数校验 ---------------------- */
    /**
     * DASHBOARD_PARAM_REPORTRECEIVERS_ISNULL
     */
    @Test
    public void addVisualDashboard_receiversIsNull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_REPORTRECEIVERS_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_REPORTRANGE_ISNULL
     */
    @Test
    public void addVisualDashboard_rangeIsNull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_REPORTRANGE_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_RANGETYPE_ISNULL
     */
    @Test
    public void addVisualDashboard_rangeTypeIsNull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_RANGETYPE_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_RANGEINTERVAL_ISNULL
     */
    @Test
    public void addVisualDashboard_rangeIntervalIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_RANGEINTERVAL_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_RANGEUNIT_ISNULL
     */
    @Test
    public void addVisualDashboard_rangeUnitIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_RANGEUNIT_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_RANGETYPE_INVALIDE
     */
    @Test
    public void addVisualDashboard_rangeTypeInvalide(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_RANGETYPE_INVALIDE);
    }

    /**
     * DASHBOARD_PARAM_RANGEUNIT_INVALIDE
     */
    @Test
    public void addVisualDashboard_rangeUnitInvalide(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_RANGEUNIT_INVALIDE);
    }

    /**
     * DASHBOARD_PARAM_REPORTSCHEDULE_ISNULL
     */
    @Test
    public void addVisualDashboard_reportScheduleIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_REPORTSCHEDULE_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_SCHEDULETYPE_ISNULL
     */
    @Test
    public void addVisualDashboard_scheduleTypeIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_SCHEDULETYPE_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_SCHEDULEREPEAT_ISNULL
     */
    @Test
    public void addVisualDashboard_scheduleRepeatIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_SCHEDULEREPEAT_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_SCHEDULESTIME_ISNULL
     */
    @Test
    public void addVisualDashboard_scheduleStimeIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_SCHEDULESTIME_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_SCHEDULETYPE_INVALIDE
     */
    @Test
    public void addVisualDashboard_scheduleTypeInvalide(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_SCHEDULETYPE_INVALIDE);
    }

    /**
     * DASHBOARD_PARAM_SCHEDULENUMS_ISNULL
     */
    @Test
    public void addVisualDashboard_scheduleNumsIsnull(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_SCHEDULENUMS_ISNULL);
    }

    /**
     * DASHBOARD_PARAM_SCHEDULENUMS_INVALIDE
     */
    @Test
    public void addVisualDashboard_scheduleNumsInvalide3(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_SCHEDULENUMS_INVALIDE);
    }

    /**
     * DASHBOARD_PARAM_SCHEDULENUMS_INVALIDE
     */
    @Test
    public void addVisualDashboard_scheduleNumsInvalide4(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_SCHEDULENUMS_INVALIDE);
    }

    /**
     * DASHBOARD_PARAM_REPORTCHARTSHARE_INVALIDE
     */
    @Test
    public void addVisualDashboard_reportChartShareInvalide(){
        VisualDashboard visualDashboard = jsonToInParameter(testName.getMethodName(), VisualDashboard.class);
        MvcResult result = UTUtil.performPost("/visual/dashboard", visualDashboard);
        UTUtil.assertEqualsRCode(result,RCode.DASHBOARD_PARAM_REPORTCHARTSHARE_INVALIDE);
    }
}
