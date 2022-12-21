package com.nis.modules.panel;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.panel.entity.VisualPanel;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class VisualPanelTest extends BaseTest<VisualPanel> {

    @Override
    protected String jsonIndex() {
        return "panel/VisualPanelTestParameter.json";
    }

    /**
     * GET /visual/panel/{id} SUCCESS
     */
    @Test
    public void queryVisualPanel_id() throws Exception {
        MvcResult result = UTUtil.performGet("/visual/panel/1", null);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);

        Map<String, Object> actualResult = JSONObject.parseObject(result.getResponse().getContentAsString(), Map.class);
        Object projectInfo = actualResult.get("data");
        Assert.assertNotNull(projectInfo);
    }

    /**
     * GET /visual/panel SUCCESS
     */
    @Test
    public void queryVisualPanel_success() throws Exception {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/visual/panel", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);

        Map<String, Object> actualResult = JSONObject.parseObject(result.getResponse().getContentAsString(), Map.class);
        Object listInfo = actualResult.get("data");
        Assert.assertNotNull(listInfo);
    }

    /**
     * POST /visual/panel SUCCESS
     */
    @Test
    public void addVisualPanel_success(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * POST /visual/panel PANEL_NAME_ISNULL
     */
    @Test
    public void addVisualPanel_nameIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_NAME_ISNULL);
    }

    /**
     * POST /visual/panel PANEL_NAME_FORMAT_ERROR
     */
    @Test
    public void addVisualPanel_nameFormatError(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_NAME_FORMAT_ERROR);
    }

    /**
     * POST /visual/panel PANEL_NAME_DUPLICATE
     */
    @Test
    public void addVisualPanel_nameDuplicate(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_NAME_DUPLICATE);
    }

    /**
     * POST /visual/panel PANEL_TYPE_ISNULL
     */
    @Test
    public void addVisualPanel_typeIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_TYPE_ISNULL);
    }

    /**
     * POST /visual/panel PANEL_TYPE_INVALIDE
     */
    @Test
    public void addVisualPanel_typeInvalide(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_TYPE_INVALIDE);
    }

    /**
     * POST /visual/panel PANEL_NOT_EXIST
     */
    @Test
    public void addVisualPanel_notExist(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_NOT_EXIST);
    }

    /**
     * POST /visual/panel PANEL_NAME_TOO_LONG
     */
    @Test
    public void addVisualPanel_error(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.CONTENT_MORETHAN_MAXLENGTH);
    }

    /**
     * PUT /visual/panel SUCCESS
     */
    @Test
    public void updateVisualPanel_success(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPut("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * PUT /visual/panel PANEL_ID_ISNULL
     */
    @Test
    public void updateVisualPanel_idIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPut("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_ID_ISNULL);
    }

    /**
     * PUT /visual/panel PANEL_NAME_TOO_LONG
     */
    @Test
    public void updateVisualPanel_error(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPut("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.CONTENT_MORETHAN_MAXLENGTH);
    }

    /**
     * DELETE /visual/panel SUCCESS
     */
    @Test
    public void deleteVisualPanel_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/visual/panel", map);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * DELETE /visual/panel PANEL_ID_ISNULL
     */
    @Test
    public void deleteVisualPanel_idIsnull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/visual/panel", map);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_ID_ISNULL);
    }

    /**
     * POST /duplicate/{id} SUCCESS
     */
    @Test
    public void duplicate_success(){
        MvcResult result = UTUtil.performPost("/visual/panel/duplicate/4", null);
        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * POST /duplicate/{id} CHART_PANEL_ISNULL
     */
    @Test
    public void duplicate_chartPanelIsnull(){
        MvcResult result = UTUtil.performPost("/visual/panel/duplicate/8", null);
        UTUtil.assertEqualsRCode(result,RCode.CHART_PANEL_ISNULL);
    }

    /**
     * POST /duplicate/{id} PANEL_NAME_TOO_LONG
     */
    @Test
    public void duplicate_panelNameTooLong(){
        MvcResult result = UTUtil.performPost("/visual/panel/duplicate/1", null);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_NAME_TOO_LONG);
    }
    /** ------------------- param 参数校验 ---------------------- */
    /**
     * PANEL_PARAM_REPORTRECEIVERS_ISNULL
     */
    @Test
    public void addVisualPanel_receiversIsNull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_REPORTRECEIVERS_ISNULL);
    }

    /**
     * PANEL_PARAM_REPORTRANGE_ISNULL
     */
    @Test
    public void addVisualPanel_rangeIsNull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_REPORTRANGE_ISNULL);
    }

    /**
     * PANEL_PARAM_RANGETYPE_ISNULL
     */
    @Test
    public void addVisualPanel_rangeTypeIsNull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_RANGETYPE_ISNULL);
    }

    /**
     * PANEL_PARAM_RANGEINTERVAL_ISNULL
     */
    @Test
    public void addVisualPanel_rangeIntervalIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_RANGEINTERVAL_ISNULL);
    }

    /**
     * PANEL_PARAM_RANGEUNIT_ISNULL
     */
    @Test
    public void addVisualPanel_rangeUnitIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_RANGEUNIT_ISNULL);
    }

    /**
     * PANEL_PARAM_RANGETYPE_INVALIDE
     */
    @Test
    public void addVisualPanel_rangeTypeInvalide(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_RANGETYPE_INVALIDE);
    }

    /**
     * PANEL_PARAM_RANGEUNIT_INVALIDE
     */
    @Test
    public void addVisualPanel_rangeUnitInvalide(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_RANGEUNIT_INVALIDE);
    }

    /**
     * PANEL_PARAM_REPORTSCHEDULE_ISNULL
     */
    @Test
    public void addVisualPanel_reportScheduleIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_REPORTSCHEDULE_ISNULL);
    }

    /**
     * PANEL_PARAM_SCHEDULETYPE_ISNULL
     */
    @Test
    public void addVisualPanel_scheduleTypeIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_SCHEDULETYPE_ISNULL);
    }

    /**
     * PANEL_PARAM_SCHEDULEREPEAT_ISNULL
     */
    @Test
    public void addVisualPanel_scheduleRepeatIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_SCHEDULEREPEAT_ISNULL);
    }

    /**
     * PANEL_PARAM_SCHEDULESTIME_ISNULL
     */
    @Test
    public void addVisualPanel_scheduleStimeIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_SCHEDULESTIME_ISNULL);
    }

    /**
     * PANEL_PARAM_SCHEDULETYPE_INVALIDE
     */
    @Test
    public void addVisualPanel_scheduleTypeInvalide(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_SCHEDULETYPE_INVALIDE);
    }

    /**
     * PANEL_PARAM_SCHEDULENUMS_ISNULL
     */
    @Test
    public void addVisualPanel_scheduleNumsIsnull(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_SCHEDULENUMS_ISNULL);
    }

    /**
     * PANEL_PARAM_SCHEDULENUMS_INVALIDE
     */
    @Test
    public void addVisualPanel_scheduleNumsInvalide3(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_SCHEDULENUMS_INVALIDE);
    }

    /**
     * PANEL_PARAM_SCHEDULENUMS_INVALIDE
     */
    @Test
    public void addVisualPanel_scheduleNumsInvalide4(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_SCHEDULENUMS_INVALIDE);
    }

    /**
     * PANEL_PARAM_REPORTCHARTSHARE_INVALIDE
     */
    @Test
    public void addVisualPanel_reportChartShareInvalide(){
        VisualPanel visualPanel = jsonToInParameter(testName.getMethodName(), VisualPanel.class);
        MvcResult result = UTUtil.performPost("/visual/panel", visualPanel);
        UTUtil.assertEqualsRCode(result,RCode.PANEL_PARAM_REPORTCHARTSHARE_INVALIDE);
    }
}
