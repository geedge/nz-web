package com.nis.modules.alert;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.alert.entity.AlertSilenceConf;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AlertSilenceTest extends BaseTest<AlertSilenceConf> {

    @Override
    protected String jsonIndex() {

        return "alert/AlertSilenceTestParameter.json";
    }

    /**
     * get  /alert/silence  exist
     */
    @Test()
    public void queryAlertSilenceList_exist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/silence", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * get  /alert/silence  not exist
     */
    @Test()
    public void queryAlertSilenceList_notexist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/silence", map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * get  /alert/silence/{id}  exist
     */
    @Test()
    public void queryAlertSilenceById_exist() throws UnsupportedEncodingException {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performGet(String.format("/alert/silence/%s", alertSilenceConf.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object alertSilence = actualResult.get("data");

        // isExist
        Assert.assertNotNull(alertSilence);
    }

    /**
     * get  /alert/silence{id}  not exist
     */
    @Test()
    public void queryAlertSilenceById_notexist() throws UnsupportedEncodingException {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performGet(String.format("/alert/silence/%s", alertSilenceConf.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object alertSilence = actualResult.get("data");

        // isExist
        Assert.assertNull(alertSilence);
    }

    /**
     * put  /alert/silence  success
     */
    @Test()
    public void updateAlertSilence_success() {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performPut("/alert/silence", alertSilenceConf);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }


    /**
     * put  /alert/silence  name is null
     */
    @Test()
    public void updateAlertSilence_nameisnull() {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performPut("/alert/silence", alertSilenceConf);

        UTUtil.assertEqualsRCode(result, RCode.ALERTSILENCE_NAME_ISNULL);
    }

    /**
     * put  /alert/silence  matchers is null
     */
    @Test()
    public void updateAlertSilence_matchersisnull() {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performPut("/alert/silence", alertSilenceConf);

        UTUtil.assertEqualsRCode(result, RCode.ALERTSILENCE_MATCHER_ISNULL);
    }

    /**
     * put  /alert/silence  startAt is null
     */
    @Test()
    public void updateAlertSilence_startAtisnull() {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performPut("/alert/silence", alertSilenceConf);

        UTUtil.assertEqualsRCode(result, RCode.ALERTSILENCE_STARTTIME_ISNULL);
    }

    /**
     * put  /alert/silence  endAt is null
     */
    @Test()
    public void updateAlertSilence_endAtisnull() {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performPut("/alert/silence", alertSilenceConf);

        UTUtil.assertEqualsRCode(result, RCode.ALERTSILENCE_ENDTIME_ISNULL);
    }

    /**
     * put  /alert/silence  endAt must great now time
     */
    @Test()
    public void updateAlertSilence_endAtmustgtnow() {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performPut("/alert/silence", alertSilenceConf);

        UTUtil.assertEqualsRCode(result, RCode.ALERTSILENCE_ENDTIME_MUSTGTNOW);
    }

    /**
     * post  /alert/silence  success
     */
    @Test()
    public void addAlertSilence_success() {
        AlertSilenceConf alertSilenceConf = jsonToInParameter(testName.getMethodName(), AlertSilenceConf.class);
        MvcResult result = UTUtil.performPost("/alert/silence", alertSilenceConf);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * delete  /alert/silence  success
     */
    @Test()
    public void deleteAlertSilence_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/alert/silence", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * delete  /alert/silence  id is null
     */
    @Test()
    public void deleteAlertSilence_idisnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/alert/silence", map);

        UTUtil.assertEqualsRCode(result, RCode.ALERTSILENCE_ID_IS_NOT_NULL);
    }


    /**
     * get  /alert/silence/{id}/rel  success
     */
    @Test()
    public void queryAlertSilenceIdRel_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet(String.format("/alert/silence/%s/rel", map.get("id")),map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }
}
