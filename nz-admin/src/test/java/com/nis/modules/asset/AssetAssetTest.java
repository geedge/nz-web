package com.nis.modules.asset;

import com.alibaba.fastjson.JSONObject;
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

}
