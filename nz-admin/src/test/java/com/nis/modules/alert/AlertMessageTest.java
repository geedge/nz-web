package com.nis.modules.alert;


import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.alert.entity.AlertMessageEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AlertMessageTest extends BaseTest<AlertMessageEntity> {

    @Override
    protected String jsonIndex() {
        return "alert/AlertMessageTestParameter.json";
    }

    /**
     * get  /alert/message exist
     */
    @Test()
    public void listMessage_exist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/message", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * get  /alert/message not exist
     */
    @Test()
    public void listMessage_notexist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/message", map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * delete  /alert/message success
     */
//    @Test()
    public void deleteMessage_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/alert/message", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * delete  /alert/message error
     */
    @Test()
    public void deleteMessage_error() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/alert/message", map);

        UTUtil.assertEqualsRCode(result, RCode.ERROR);
    }

    /**
     * get  /alert/message/{id}  exist
     */
    @Test()
    public void queryInfoMessage_exist() throws Exception {
        AlertMessageEntity alertMessageEntity = jsonToInParameter(testName.getMethodName(), AlertMessageEntity.class);
        MvcResult result = UTUtil.performGet(String.format("/alert/message/%s", alertMessageEntity.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object ipamSubnetInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(ipamSubnetInfo);
    }

    /**
     * get  /alert/message/{id} not exist
     */
    @Test()
    public void queryInfoMessage_notexist() throws Exception {
        AlertMessageEntity alertMessageEntity = jsonToInParameter(testName.getMethodName(), AlertMessageEntity.class);
        MvcResult result = UTUtil.performGet(String.format("/alert/message/%s", alertMessageEntity.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object ipamSubnetInfo = actualResult.get("data");

        // isExist
        Assert.assertNull(ipamSubnetInfo);
    }


    /**
     * get  /alert/message  exist
     */
    @Test()
    public void listMessageRel_exist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/message/rel", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * get  /alert/message not exist
     */
//    @Test()
    public void listMessageRel_notexist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/message/rel", map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * get  /alert/message csv
     */
    @Test()
    public void messageExportFile_csv() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/message/export", map);

        UTUtil.assertToExportFile(result, map,2);
    }

    /**
     * get  /alert/message json
     */
    @Test()
    public void messageExportFile_json() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/message/export", map);

        UTUtil.assertToExportFile(result, map,2);
    }

    /**
     * get  /alert/message excel
     */
    @Test()
    public void messageExportFile_excel() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/alert/message/export", map);

        UTUtil.assertToExportFile(result, map,2);
    }


}
