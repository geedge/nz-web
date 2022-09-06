package com.nis.modules.asset;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.asset.entity.AssetAsset;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest
public class AssetAssetTest extends BaseTest<AssetAsset> {

    @Override
    protected String jsonIndex() {
        return "asset/AssetAssetTestParameter.json";
    }


    /**
     * GET /asset/asset/id
     *
     * @throws Exception
     */
    @Test
    public void queryAssetInfo_ID2() throws Exception {
        MvcResult result = UTUtil.performGet("/asset/asset/2", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object projectInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(projectInfo);

        List<Map<String, Object>> alert = (List) ((Map) actualResult.get("data")).get("alert");

        String p1Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P1", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertNotEquals("0", p1Alert);

        String p2Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P2", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertNotEquals("0", p2Alert);

        String p3Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P3", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertNotEquals("0", p3Alert);
    }

    /**
     * GET /asset/asset/id
     *
     * @throws Exception
     */
    @Test
    public void queryAssetInfo_ID3() throws Exception {
        MvcResult result = UTUtil.performGet("/asset/asset/3", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object projectInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(projectInfo);

        List<Map<String, Object>> alert = (List) ((Map) actualResult.get("data")).get("alert");

        String p1Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P1", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertEquals("0", p1Alert);

        String p2Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P2", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertEquals("0", p2Alert);

        String p3Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P3", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertEquals("0", p3Alert);
    }


    /**
     * GET /asset/asset/  收藏
     *
     * @throws Exception
     */
    @Test
    public void queryAssetList_ID1(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/asset/asset", map);

        UTUtil.assertToSizeByFlag(result, true);
    }




    /**
     * GET /asset/comment  资产评论
     *
     * @throws Exception
     */
    @Test
    public void queryAssetCommentListByParams_success() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/asset/comment", map);

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
     * POST /asset/comment  SUCCESS
     */
    @Test
    public void addAssetComment_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/asset/comment", map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }
    /**
     * POST /asset/comment  ASSET_ID_ISNULL
     */
    @Test
    public void addAssetComment_assetIdIsnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/asset/comment", map);
        UTUtil.assertEqualsRCode(result, RCode.ASSET_ID_ISNULL);
    }
    /**
     * POST /asset/comment  ASSET_COMMENT_CONTENT_ISNULL
     */
    @Test
    public void addAssetComment_contentIsnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/asset/comment", map);
        UTUtil.assertEqualsRCode(result, RCode.ASSET_COMMENT_CONTENT_ISNULL);
    }
    /**
     * PUT /asset/comment  SUCCESS
     */
    @Test
    public void editAssetComment_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/asset/comment", map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }
    /**
     * PUT /asset/comment  ASSET_COMMENT_ID_ISNULL
     */
    @Test
    public void editAssetComment_commentIdIsnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/asset/comment", map);
        UTUtil.assertEqualsRCode(result, RCode.ASSET_COMMENT_ID_ISNULL);
    }
    /**
     * DELETE /asset/comment  SUCCESS
     */
    @Test
    public void delAssetComment_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/asset/comment", map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }
    /**
     * DELETE /asset/comment  ASSET_COMMENT_ID_ISNULL
     */
    @Test
    public void delAssetComment_commentIdIsnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/asset/comment", map);
        UTUtil.assertEqualsRCode(result, RCode.ASSET_COMMENT_ID_ISNULL);
    }
}
