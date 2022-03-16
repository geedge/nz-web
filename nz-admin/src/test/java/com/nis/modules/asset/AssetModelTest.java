package com.nis.modules.asset;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.asset.entity.AssetModel;
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
public class AssetModelTest extends BaseTest<AssetModel> {

    @Override
    protected String jsonIndex() {
        return "asset/AssetModelTestParameter.json";
    }

    /**
     * POST  /asset/model success
     */
    @Test()
    public void saveModel_success() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST  /asset/model check name not null
     */
    @Test()
    public void saveModel_checkNameNotNull() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_MODELNAME_ISNULL);
    }

    /**
     * POST  /asset/model check brand id not null
     */
    @Test()
    public void saveModel_checkBrandIdNotNull() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_MODEL_BRANDID_ISNULL);
    }

    /**
     * POST  /asset/model check type id not null
     */
    @Test()
    public void saveModel_checkTypeIdNotNull() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_TYPE_ID_ISNULL);
    }

    /**
     * POST  /asset/model check brand exist
     */
    @Test()
    public void saveModel_checkBrandExist() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_BRAND_NOT_FOUND);
    }

    /**
     * POST  /asset/model check name duplicate
     */
    @Test()
    public void saveModel_checkNameDuplicate() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_MODELNAME_DUPLICATE);
    }

    /**
     * POST  /asset/model check type exist
     */
    @Test()
    public void saveModel_checkTypeExist() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.TYPECONF_NOT_FOUND);
    }

    /**
     * POST  /asset/model check sysobjectid format
     */
    @Test()
    public void saveModel_checkOIDFormat() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_MODEL_OID_INCORRECT);
    }

    /**
     * POST  /asset/model check sysobjectid duplicate
     */
    @Test()
    public void saveModel_checkOIDDuplicate() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPost("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_MODEL_OID_DUPLICATE);
    }

    /**
     * PUT /asset/model success
     */
    @Test()
    public void updateModel_success() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPut("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /asset/model check name duplicate
     */
    @Test()
    public void updateModel_checkNameDuplicate() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPut("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_MODELNAME_DUPLICATE);
    }

    /**
     * PUT /asset/model check oid duplicate
     */
    @Test()
    public void updateModel_checkOIDDuplicate() {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performPut("/asset/model", assetModel);

        UTUtil.assertEqualsRCode(result, RCode.ASSET_MODEL_OID_DUPLICATE);
    }


    /**
     * GET /asset/model/id  model not exist
     *
     * @throws Exception
     */
    @Test
    public void queryModelInfo_notexist() throws Exception {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performGet(String.format("/asset/model/%s", assetModel.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object modelInfo = actualResult.get("data");

        // isExist
        Assert.assertNull(modelInfo);
    }


    /**
     * GET /asset/model/id model exist
     *
     * @throws Exception
     */
    @Test
    public void queryModelInfo_exist() throws Exception {
        AssetModel assetModel = jsonToInParameter(testName.getMethodName(), AssetModel.class);
        MvcResult result = UTUtil.performGet(String.format("/asset/model/%s", assetModel.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object modelInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(modelInfo);
    }


    /**
     * GET /asset/model not exist
     */
    @Test
    public void queryModelList_notexist() {
        Map queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/asset/model", queryParam);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * GET /asset/model  exist
     */
    @Test
    public void queryModelList_exist() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/asset/model", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * DELEET /asset/model success
     */
    @Test
    public void deleteModel_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/asset/model", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }


}
