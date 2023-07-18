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

        String p3Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P2", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
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
}
