package com.nis.modules.asset;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.asset.entity.AssetTypeConf;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest
public class AssetTypeConfTest extends BaseTest<AssetTypeConf> {

    @Override
    protected String jsonIndex() {
        return "asset/AssetTypeConfTestParameter.json";
    }

    /**
     * POST  /asset/typeConf success
     */
    @Test()
    public void saveConf_success() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST  /asset/typeConf check name not null
     */
    @Test()
    public void saveConf_checkNameNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_NAME_ISNULL);
    }

    /**
     * POST  /asset/typeConf check name duplicate
     */
    @Test()
    public void saveConf_checkNameDuplicate() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_NAME_DUPLICATE);
    }

    /**
     * POST  /asset/typeConf check vm not null
     */
    @Test()
    public void saveConf_checkVmNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_VM_ISNULL);
    }

    /**
     * POST  /asset/typeConf check vm Incorrect
     */
    @Test()
    public void saveConf_checkVmIncorrect() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_VM_INCORRECT);
    }

    /**
     * POST  /asset/typeConf check vmh not null
     */
    @Test()
    public void saveConf_checkVmhNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_VMH_ISNULL);
    }

    /**
     * POST  /asset/typeConf check vmh Incorrect
     */
    @Test()
    public void saveConf_checkVmhIncorrect() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_VMH_INCORRECT);
    }

    /**
     * POST  /asset/typeConf check AuthProtocol Incorrect
     */
    @Test()
    public void saveConf_checkAuthProtocolNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_AUTHPROTOCOL_ISNULL);
    }

    /**
     * POST  /asset/typeConf check AuthProtocol Incorrect
     */
    @Test()
    public void saveConf_checkAuthProtocolIncorrect() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_AUTHPROTOCOL_INCORRECT);
    }

    /**
     * POST  /asset/typeConf check SnmpEnable Incorrect
     */
    @Test()
    public void saveConf_checkSnmpEnableNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_SNMPENABLE_ISNULL);
    }

    /**
     * POST  /asset/typeConf check SnmpEnable Incorrect
     */
    @Test()
    public void saveConf_checkSnmpEnableIncorrect() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_SNMPENABLE_INCORRECT);
    }

    /**
     * POST  /asset/typeConf check ClientEnable Incorrect
     */
    @Test()
    public void saveConf_checkClientEnableNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_CLIENTENABLE_ISNULL);
    }

    /**
     * POST  /asset/typeConf check ClientEnable Incorrect
     */
    @Test()
    public void saveConf_checkClientEnableIncorrect() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_CLIENTENABLE_INCORRECT);
    }

    /**
     * POST  /asset/typeConf check SnmpCollect Incorrect
     */
    @Test()
    public void saveConf_checkSnmpCollectNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_SNMPCOLLECT_ISNULL);
    }

    /**
     * POST  /asset/typeConf check snmpCollect Incorrect
     */
    @Test()
    public void saveConf_checkSnmpCollectIncorrect() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_SNMPCOLLECT_INCORRECT);
    }

    /**
     * POST  /asset/typeConf check SshCollect not null
     */
    @Test()
    public void saveConf_checkSshCollectNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_SSHCOLLECT_ISNULL);
    }

    /**
     * POST  /asset/typeConf check SshCollect Incorrect
     */
    @Test()
    public void saveConf_checkSshCollectIncorrect() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_SSHCOLLECT_INCORRECT);
    }

    /**
     * POST  /asset/typeConf check SshCollectScript not null
     */
    @Test()
    public void saveConf_checkSshCollectScriptNotNull() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPost("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_SSHCOLLECTSCRIPT_ISNULL);
    }

    /**
     * PUT /asset/typeConf success
     */
    @Test()
    public void updateConf_success() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPut("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /asset/typeConf check name duplicate
     */
    @Test()
    public void updateConf_checkNameDuplicate() {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performPut("/asset/typeConf", assetTypeConf);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_NAME_DUPLICATE);
    }


    /**
     * GET /asset/typeConf/id  conf not exist
     *
     * @throws Exception
     */
    @Test
    public void queryConfInfo_notexist() throws Exception {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performGet(String.format("/asset/typeConf/%s", assetTypeConf.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object modelInfo = actualResult.get("data");

        // isExist
        Assert.assertNull(modelInfo);
    }


    /**
     * GET /asset/typeConf/id conf exist
     *
     * @throws Exception
     */
    @Test
    public void queryConfInfo_exist() throws Exception {
        AssetTypeConf assetTypeConf = jsonToInParameter(testName.getMethodName(), AssetTypeConf.class);
        MvcResult result = UTUtil.performGet(String.format("/asset/typeConf/%s", assetTypeConf.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object modelInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(modelInfo);
    }


    /**
     * GET /asset/typeConf not exist
     */
    @Test
    public void queryConfList_notexist() {
        Map queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/asset/typeConf", queryParam);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * GET /asset/typeConf  exist
     */
    @Test
    public void queryConfList_exist() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/asset/typeConf", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * DELEET /asset/typeConf success
     */
    @Test
    public void deleteConf_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/asset/typeConf", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }
    /**
     * DELEET /asset/typeConf success
     */
    @Test
    public void deleteConf_buildIn() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/asset/typeConf", map);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_BUILDIN_CAN_NOT_REMOVE);
    }


}
