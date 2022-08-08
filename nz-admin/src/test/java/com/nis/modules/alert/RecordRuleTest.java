package com.nis.modules.alert;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.alert.entity.RecordRuleEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;


@RunWith(SpringRunner.class)
@SpringBootTest
public class RecordRuleTest extends BaseTest<RecordRuleEntity> {

    @Override
    protected String jsonIndex() {
        return "alert/RecordRuleTestParameter.json";
    }

    /**
     * GET /record/rule/{id} exist
     */
    @Test
    public void queryRecordRuleById() throws UnsupportedEncodingException {
        MvcResult result = UTUtil.performGet("/record/rule/1", null);
        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object projectInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(projectInfo);
    }

    /**
     * GET /record/rule success
     */
    @Test
    public void queryRecordRuleList_success() throws UnsupportedEncodingException {
        MvcResult result = UTUtil.performGet("/record/rule", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Map resultInfo = (Map)actualResult.get("data");
        Assert.assertNotNull(resultInfo);

        Object list = resultInfo.get("list");
        Assert.assertNotNull(list);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * GET /record/rule success
     */
    @Test
    public void queryRecordRuleListByParams_success() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Map resultInfo = (Map)actualResult.get("data");
        Assert.assertNotNull(resultInfo);

        Object list = resultInfo.get("list");
        Assert.assertNotNull(list);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * GET /record/rule   not exist
     */
    @Test
    public void queryRecordRuleListByParams_notExist() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Map resultInfo = (Map)actualResult.get("data");
        Assert.assertNotNull(resultInfo);

        List list = (List)resultInfo.get("list");
        Assert.assertEquals(0,list.size());

    }

    /**
     * POST /record/rule success
     */
    @Test
    public void addRecordRule_success() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /record/rule name is null
     */
    @Test
    public void addRecordRule_nameIsNull() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.RECORD_RULE_NAME_ISNULL);
    }

    /**
     * POST /record/rule type is null
     */
    @Test
    public void addRecordRule_typeIsNull() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.RECORD_RULE_TYPE_ISNULL);
    }

    /**
     *  POST /record/rule labels is null
     */
    @Test
    public void addRecordRule_labelsIsNull() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.RECORD_RULE_LABELS_ISNULL);
    }

    /**
     * POST /record/rule state is null
     */
    @Test
    public void addRecordRule_stateIsNull() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.RECORD_RULE_STATE_ISNULL);
    }

    /**
     * POST /record/rule type is error
     */
    @Test
    public void addRecordRule_typeIsError() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.RECORD_RULE_TYPE_ERROR);
    }

    /**
     * POST /record/rule state is error
     */
    @Test
    public void addRecordRule_stateIsError() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.RECORD_RULE_STATE_ERROR);
    }

    /**
     * PUT /record/rule success
     */
    @Test
    public void updateRecordRule_success() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /record/rule id is null
     */
    @Test
    public void updateRecordRule_idIsNull() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.RECORD_RULE_ID_ISNULL);
    }


    /**
     * DELETE /record/rule success
     */
    @Test
    public void deleteRecordRule_success() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }



    /**
     * DELETE /record/rule buildIn
     */
    @Test
    public void deleteRecordRule_buildIn() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/record/rule", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        UTUtil.assertEqualsRCode(result, RCode.RECORD_RULE_BUILD_IN_EXIST);
    }
}
