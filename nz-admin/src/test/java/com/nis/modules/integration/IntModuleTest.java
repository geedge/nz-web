package com.nis.modules.integration;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Tool;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.integration.entity.IntAlertRuleEntity;
import com.nis.modules.integration.entity.IntMetadataEntity;
import com.nis.modules.integration.entity.IntModuleEntity;
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
public class IntModuleTest extends BaseTest<IntModuleEntity> {
    @Override
    protected String jsonIndex() {
        return "integration/IntModuleTestParameter.json";
    }

    /**
     * GET /integration/module/id
     *
     * @throws Exception
     */
    @Test
    public void queryIntModuleInfo_ID1() throws Exception {
        MvcResult result = UTUtil.performGet("/integration/module/1", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intModuleResult = JSONObject.parseObject(responseStr, Map.class);
        Object data = intModuleResult.get("data");
        IntModuleEntity moduleEntity = Tool.BeanUtil.toBean(data, IntModuleEntity.class);

        Assert.assertNotNull(moduleEntity);
    }

    /**
     * GET /integration/module/id
     *
     * @throws Exception
     */
    @Test
    public void queryIntModuleInfo_ID2() throws Exception {
        MvcResult result = UTUtil.performGet("/integration/module/2", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intModuleResult = JSONObject.parseObject(responseStr, Map.class);
        Object data = intModuleResult.get("data");
        IntModuleEntity moduleEntity = Tool.BeanUtil.toBean(data, IntModuleEntity.class);

        List<IntAlertRuleEntity> alertRules = moduleEntity.getAlertRules();
        Assert.assertEquals(alertRules.size(), 0);

    }

    /**
     * GET /integration/module/id
     *
     * @throws Exception
     */
    @Test
    public void queryIntModuleInfo_ID3() throws Exception {
        MvcResult result = UTUtil.performGet("/integration/module/3", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intModuleResult = JSONObject.parseObject(responseStr, Map.class);
        Object data = intModuleResult.get("data");
        IntModuleEntity moduleEntity = Tool.BeanUtil.toBean(data, IntModuleEntity.class);

        List<IntMetadataEntity> metadatas = moduleEntity.getMetadatas();
        Assert.assertNotNull(metadatas);
    }

    /**
     * GET /integration/module success
     *
     * @throws Exception
     */
    @Test
    public void queryIntModule_success() throws Exception {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/integration/module", queryParam);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intModuleResult = JSONObject.parseObject(responseStr, Map.class);
        Map data = (Map) intModuleResult.get("data");
        Object moduleList = data.get("list");

        Assert.assertNotNull(moduleList);
    }

    /**
     * GET /integration/module data is null
     *
     * @throws Exception
     */
    @Test
    public void queryIntModule_null() throws Exception {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/integration/module", queryParam);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intModuleResult = JSONObject.parseObject(responseStr, Map.class);
        Object data = intModuleResult.get("list");

        Assert.assertNull(data);
    }

    /**
     * GET /integration/metadata success
     *
     * @throws Exception
     */
    @Test
    public void queryIntMetadata_success() throws Exception {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/integration/metadata", queryParam);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intMetadataResult = JSONObject.parseObject(responseStr, Map.class);
        Map data = (Map) intMetadataResult.get("data");
        Object metadataList = data.get("list");

        Assert.assertNotNull(metadataList);
    }

    /**
     * GET /integration/metadata data is null
     *
     * @throws Exception
     */
    @Test
    public void queryIntMetadata_null() throws Exception {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/integration/metadata", queryParam);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intMetadataResult = JSONObject.parseObject(responseStr, Map.class);
        Map data = (Map) intMetadataResult.get("data");
        JSONArray list = (JSONArray) data.get("list");

        Assert.assertEquals(list.size(), 0);
    }

    /**
     * GET /integration/alertRule success
     *
     * @throws Exception
     */
    @Test
    public void queryIntAlertRule_success() throws Exception {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/integration/alertRule", queryParam);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intAlertRuleResult = JSONObject.parseObject(responseStr, Map.class);
        Map data = (Map) intAlertRuleResult.get("data");
        Object alertRuleList = data.get("list");

        Assert.assertNotNull(alertRuleList);
    }


    /**
     * GET /integration/alertRule data is null
     *
     * @throws Exception
     */
    @Test
    public void queryIntAlertRule_null() throws Exception {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/integration/alertRule", queryParam);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> intAlertRuleResult = JSONObject.parseObject(responseStr, Map.class);
        Map data = (Map) intAlertRuleResult.get("data");
        JSONArray list = (JSONArray) data.get("list");

        Assert.assertEquals(list.size(), 0);
    }


}
