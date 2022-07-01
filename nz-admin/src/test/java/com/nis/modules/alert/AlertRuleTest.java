package com.nis.modules.alert;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.alert.entity.AlertRuleEntity;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest
public class AlertRuleTest extends BaseTest<AlertRuleEntity> {

    @Override
    protected String jsonIndex() {
        return "alert/AlertRuleTestParameter.json";
    }

    /**
     * GET /alert/rule/id
     *
     * @throws Exception
     */
    @Test
    public void queryAlertRuleInfo_ID1() throws Exception {
        MvcResult result = UTUtil.performGet("/alert/rule/1", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object alertInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(alertInfo);

        List<Map<String, Object>> alert = (List) ((Map) actualResult.get("data")).get("alert");

        String p3Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P3", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertNotEquals("0", p3Alert);
    }


    /**
     * GET /alert/rule/id
     *
     * @throws Exception
     */
    @Test
    public void queryAlertRuleInfo_ID2() throws Exception {
        MvcResult result = UTUtil.performGet("/alert/rule/2", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object alertInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(alertInfo);

        List<Map<String, Object>> alert = (List) ((Map) actualResult.get("data")).get("alert");

        String p1Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P1", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertEquals("0", p1Alert);

        String p2Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P2", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertEquals("0", p2Alert);

        String p3Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P3", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertEquals("0", p3Alert);
    }


    /**
     * GET /alert/rule/id
     *
     * @throws Exception
     */
    @Test
    public void queryAlertRuleInfo_ID3() throws Exception {
        MvcResult result = UTUtil.performGet("/alert/rule/3", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object alertInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(alertInfo);

        // 收藏
        Integer starred = (Integer) ((Map) actualResult.get("data")).get("starred");
        Assert.assertEquals("1", starred.toString());
    }


    /**
     * GET /alert/rule/id
     *
     * @throws Exception
     */
    @Test
    public void queryAlertRuleInfo_ID5() throws Exception {
        MvcResult result = UTUtil.performGet("/alert/rule/5", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object alertInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(alertInfo);

        // 非收藏
        Integer starred = (Integer) ((Map) actualResult.get("data")).get("starred");
        Assert.assertEquals("0", starred.toString());
    }


    /**
     * GET /alert/rule queryRuleList_starred
     */
    @Test
    public void queryRuleList_starred() {
        Map queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/rule", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * DELETE /alert/rule success
     */
    @Test
    public void DeleteRuleList_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/alert/rule", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /alert/rule success
     */
    @Test
    public void saveAlertRule_success() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /alert/rule ALERTRULE_NAME_ISNULL
     */
    @Test
    public void saveAlertRule_nameIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_NAME_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_EXPR_ISNULL
     */
    @Test
    public void saveAlertRule_exprIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_EXPR_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_LAST_ISNULL
     */
    @Test
    public void saveAlertRule_lastIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_LAST_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_SUMMARY_ISNULL
     */
    @Test
    public void saveAlertRule_summaryIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_SUMMARY_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_TYPE_ISNULL
     */
    @Test
    public void saveAlertRule_typeIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_TYPE_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_TYPE_INCORRECT
     */
    @Test
    public void saveAlertRule_typeIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_TYPE_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_CONDITION_ISNULL
     */
    @Test
    public void saveAlertRule_conditionIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_CONDITION_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_OPERATOR_ISNULL
     */
    @Test
    public void saveAlertRule_operatorIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_OPERATOR_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_OPERATOR_INVALIDE
     */
    @Test
    public void saveAlertRule_operatorInvalide() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_OPERATOR_INVALIDE);
    }

    /**
     * POST /alert/rule ALERTRULE_THRESHOLD_FORMAT
     */
    @Test
    public void saveAlertRule_thresholdFormat() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_THRESHOLD_FORMAT);
    }

    /**
     * POST /alert/rule ALERTRULE_SEVERITY_ISNULL
     */
    @Test
    public void saveAlertRule_severityIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_SEVERITY_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_SEVERITY_INCORRECT
     */
    @Test
    public void saveAlertRule_severityIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_SEVERITY_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_CONDITION_FORMAT
     */
    @Test
    public void saveAlertRule_conditionFormat() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_CONDITION_FORMAT);
    }

    /**
     * POST /alert/rule ALERTRULE_OIDFORMAT_ERROR
     */
    @Test
    public void saveAlertRule_oidformatError() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_OIDFORMAT_ERROR);
    }

    /**
     * POST /alert/rule ALERTRULE_UNIT_ISNULL
     */
    @Test
    public void saveAlertRule_unitIsnull() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_UNIT_ISNULL);
    }

    /**
     * POST /alert/rule ALERTRULE_UNIT_INVALIDE
     */
    @Test
    public void saveAlertRule_unitInvalide() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_UNIT_INVALIDE);
    }

    /**
     * POST /alert/rule ALERTRULE_METHOD_NOTFOUND
     */
    @Test
    public void saveAlertRule_methodNotfound() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_METHOD_NOTFOUND);
    }

    /**
     * POST /alert/rule ALERTRULE_STATE_INCORRECT
     */
    @Test
    public void saveAlertRule_stateIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_STATE_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_AUTOEXPIRED_INCORRECT
     */
    @Test
    public void saveAlertRule_autoexpiredIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_AUTOEXPIRED_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_NOTIFYACTIVE_INCORRECT
     */
    @Test
    public void saveAlertRule_notifyactiveIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_NOTIFYACTIVE_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_NOTIFYEXPIRED_INCORRECT
     */
    @Test
    public void saveAlertRule_notifyexpiredIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_NOTIFYEXPIRED_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_SCHEDENABLE_INCORRECT
     */
    @Test
    public void saveAlertRule_schedenableIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_SCHEDENABLE_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_SCHEDDAYS_INCORRECT
     */
    @Test
    public void saveAlertRule_scheddaysIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_SCHEDDAYS_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_SCHEDSTIME_INCORRECT
     */
    @Test
    public void saveAlertRule_schedstimeIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_SCHEDSTIME_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_SCHEDETIME_INCORRECT
     */
    @Test
    public void saveAlertRule_schedetimeIncorrect() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_SCHEDETIME_INCORRECT);
    }

    /**
     * POST /alert/rule ALERTRULE_SCHEDETIME_ERROR
     */
    @Test
    public void saveAlertRule_schedetimeError() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_SCHEDETIME_ERROR);
    }

    /**
     * POST /alert/rule ALERTRULE_NAME_DUPLICATE
     */
    @Test
    public void saveAlertRule_nameDuplicate() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_NAME_DUPLICATE);
    }

    /**
     * POST /alert/rule ALERTRULE_EXPR_ERROR
     */
    @Test
    public void saveAlertRule_exprError() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_EXPR_ERROR);
    }

    /**
     * POST /alert/rule ALERTRULE_RECEIVER_FORMAT
     */
    @Test
    public void saveAlertRule_receiverFormat() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_RECEIVER_FORMAT);
    }

    /**
     * POST /alert/rule SYS_USER_NOT_EXIST
     */
    @Test
    public void saveAlertRule_userNotExist() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPost("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.SYS_USER_NOT_EXIST);
    }

    /**
     * PUT /alert/rule SUCCESS
     */
    @Test
    public void editAlertRule_success() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPut("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /alert/rule ALERTRULE_BUILDIN_CAN_NOT_UPDATE
     */
    @Test
    public void editAlertRule_buildinCanNotUpdate() {
        AlertRuleEntity alertRule = jsonToInParameter(testName.getMethodName(), AlertRuleEntity.class);
        MvcResult result = UTUtil.performPut("/alert/rule", alertRule);

        UTUtil.assertEqualsRCode(result, RCode.ALERTRULE_BUILDIN_CAN_NOT_UPDATE);
    }
}
