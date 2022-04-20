package com.nis.modules.ipam;

import java.util.Map;

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
public class IpamSubnetTest extends BaseTest<IpamSubnet> {

    @Override
    protected String jsonIndex() {
        return "ipam/IpamSubnetTestParameter.json";
    }

    /**
     * POST  /ipam/subnet success
     */
//    @Test()
    public void saveIpamSubnet_success() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST  /ipam/subnet  name can not be empty
     */
    @Test()
    public void saveIpamSubnet_checkName() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_NAME_ISNULL);
    }

    /**
     * POST  /ipam/subnet  type can not be empty
     */
    @Test()
    public void saveIpamSubnet_checkType() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_TYPE_ISNULL);
    }

    /**
     * POST  /ipam/subnet type error must 4 or 6
     */
    @Test()
    public void saveIpamSubnet_checkTypeCorrect() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_TYPE_ERROR);
    }

    /**
     * POST  /ipam/subnet dcId can not be null
     */
    @Test()
    public void saveIpamSubnet_checkDc() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_DCID_ISNULL);
    }

    /**
     * POST  /ipam/subnet dcId not exists
     */
    @Test()
    public void saveIpamSubnet_checkDcCorrect() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_DC_ISNULL);
    }

    /**
     * POST  /ipam/subnet addr can not be null
     */
    @Test()
    public void saveIpamSubnet_checkAddr() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_ADDR_ISNULL);
    }
    
    /**
     * POST  /ipam/subnet IPv4 addr format error 
     */
    @Test()
    public void saveIpamSubnet_checkV4AddrCorrect() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_ADDR_ERROR);
    }
    
    /**
     * POST  /ipam/subnet IPv6 addr format error 
     */
    @Test()
    public void saveIpamSubnet_checkV6AddrCorrect() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_ADDR_ERROR);
    }
    
    /**
     * POST  /ipam/subnet  mask can not be null
     */
    @Test()
    public void saveIpamSubnet_checkMask() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_MASK_ISNULL);
    }
    
    /**
     * POST  /ipam/subnet IPv4 mask error
     */
    @Test()
    public void saveIpamSubnet_checkV4MaskCorrect() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_MASK_ERROR);
    }
    
    /**
     * POST  /ipam/subnet IPv6 mask error
     */
    @Test()
    public void saveIpamSubnet_checkV6MaskCorrect() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPost("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_MASK_ERROR);
    }

    /**
     * PUT /ipam/subnet success
     */
    @Test()
    public void updateIpamSubnet_success() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPut("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT  /ipam/subnet  id can not be null
     */
    @Test()
    public void updateIpamSubnet_checkId() {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performPut("/ipam/subnet", ipamSubnet);

        UTUtil.assertEqualsRCode(result, RCode.IPAM_SUBNET_ID_ISNULL);
    }

    /**
     * GET /ipam/subnet/id  exist
     *
     * @throws Exception
     */
    @Test
    public void queryIpamSubnetInfo_exist() throws Exception {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performGet(String.format("/ipam/subnet/%s", ipamSubnet.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object ipamSubnetInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(ipamSubnetInfo);
    }

    /**
     * GET /ipam/subnet/id  not exist
     *
     * @throws Exception
     */
    @Test
    public void queryIpamSubnetInfo_notexist() throws Exception {
        IpamSubnet ipamSubnet = jsonToInParameter(testName.getMethodName(), IpamSubnet.class);
        MvcResult result = UTUtil.performGet(String.format("/ipam/subnet/%s", ipamSubnet.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object ipamSubnetInfo = actualResult.get("data");

        // isExist
        Assert.assertNull(ipamSubnetInfo);
    }


    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_exist() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /ipam/subnet not exist
     */
    @Test
    public void queryIpamSubnetList_notexist() {
        Map queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_name() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }
    
    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_ids() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }
    
    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_type() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }
    
    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_dcId() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_addr() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }
    
    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_mask() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }
    
    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_vlan() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }
    
    /**
     * GET /ipam/subnet exist
     */
    @Test
    public void queryIpamSubnetList_orderBy() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/ipam/subnet", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }
    
    /**
     * DELEET /ipam/subnet success
     */
    @Test
    public void deleteIpamSubnet_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/ipam/subnet", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }
    
}
