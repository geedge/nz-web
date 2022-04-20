package com.nis.modules.ipam;

import java.util.Map;

import com.nis.modules.ipam.entity.IpamIp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.ipam.entity.IpamSubnet;

/**
 * @FixMethodOrder You should never write tests that need to be executed in a specified order. That's really bad practice. Every test should be able to run independent.
 * 执行特定顺序对于单元测试来说是一种不好的做法，但对于集成测试来说不一定是一种不好的做法。
 * 最好的选择（不理想）是用注释类 @FixMethodOrder(MethodSorters.NAME_ASCENDING)，执行顺序按字母顺序执行
 * 根据需要可命名 例如 t1_firstTest()，t2_secondTest()
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class IpamIpTest extends BaseTest<IpamIp> {

    @Override
    protected String jsonIndex() {
        return "ipam/IpamIpTestParameter.json";
    }

    /**
     * GET /ipam/ip/id  exist
     *
     * @throws Exception
     */
    @Test
    public void queryIpamIpInfo_exist() throws Exception {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performGet(String.format("/ipam/ip/%s", ipamIp.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object ipamSubnetInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(ipamSubnetInfo);
    }

    /**
     * GET /ipam/ip/id  not exist
     *
     * @throws Exception
     */
    @Test
    public void queryIpamIpInfo_notexist() throws Exception {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performGet(String.format("/ipam/ip/%s", ipamIp.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object ipamSubnetInfo = actualResult.get("data");

        // isExist
        Assert.assertNull(ipamSubnetInfo);
    }

    /**
     * GET /ipam/ip success
     *
     * @throws Exception
     */
    @Test
    public void queryIpamIpList_success(){
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/ip", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /ipam/ip error
     *
     * @throws Exception
     */
    @Test
    public void queryIpamIpList_error(){
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/ip", queryParam);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * GET /ipam/ip success
     *
     * @throws Exception
     */
    @Test
    public void queryIpamIpList_ids(){
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/ip", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /ipam/ip success
     *
     * @throws Exception
     */
    @Test
    public void queryIpamIpList_name(){
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/ip", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /ipam/ip success
     *
     * @throws Exception
     */
    @Test
    public void queryIpamIpList_state(){
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/ip", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /ipam/ip success
     *
     * @throws Exception
     */
    @Test
    public void queryIpamIpList_addr(){
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/ip", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * POST  /ipam/ip success
     */
//    @Test()
    public void saveIpamIp_success() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPost("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST  /ipam/ip name isnull
     */
//    @Test()
    public void saveIpamIp_nameisnull() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPost("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_IP_NAME_ISNULL);
    }

    /**
     * POST  /ipam/ip addr isnull
     */
    @Test()
    public void saveIpamIp_addrisnull() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPost("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_IP_ADDR_ISNULL);
    }

    /**
     * POST  /ipam/ip mac isnull
     */
//    @Test()
    public void saveIpamIp_macisnull() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPost("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_IP_MAC_ISNULL);
    }

    /**
     * POST  /ipam/ip addr error
     */
    @Test()
    public void saveIpamIp_addrerror() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPost("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_IP_ADDR_ERROR);
    }

    /**
     * POST  /ipam/ip subnet isnull
     */
    @Test()
    public void saveIpamIp_subnetisnull() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPost("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_IP_SUBNET_ISNULL);
    }

    /**
     * POST  /ipam/ip mac error
     */
//    @Test()
    public void saveIpamIp_macerror() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPost("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_IP_MAC_ERROR);
    }

    /**
     * POST  /ipam/ip ip mac exists
     */
//    @Test()
    public void saveIpamIp_ipmacexists() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPost("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_IP_INTERVAL_ERROR);
    }

    /**
     * put  /ipam/ip success
     */
//    @Test()
    public void updateIpamIp_success() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPut("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * put  /ipam/ip id is null
     */
    @Test
    public void updateIpamIp_idisnull() {
        IpamIp ipamIp = jsonToInParameter(testName.getMethodName(), IpamIp.class);
        MvcResult result = UTUtil.performPut("/ipam/ip", ipamIp);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_IP_ID_ISNULL);
    }

    /**
     * delete  /ipam/ip success
     */
    @Test
    public void deleteIpamIp_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/ipam/ip", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

}
