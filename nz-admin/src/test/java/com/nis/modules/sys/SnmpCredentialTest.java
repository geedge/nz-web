package com.nis.modules.sys;


import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.sys.entity.SnmpCredential;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.Map;


/**
 * @FixMethodOrder You should never write tests that need to be executed in a specified order. That's really bad practice. Every test should be able to run independent.
 * 执行特定顺序对于单元测试来说是一种不好的做法，但对于集成测试来说不一定是一种不好的做法。
 * 最好的选择（不理想）是用注释类 @FixMethodOrder(MethodSorters.NAME_ASCENDING)，执行顺序按字母顺序执行
 * 根据需要可命名 例如 t1_firstTest()，t2_secondTest()
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SnmpCredentialTest extends BaseTest<SnmpCredential> {

    @Override
    protected String jsonIndex() {
        return "sys/SnmpCredentialTestParameter.json";
    }

    /**
     * GET /snmp/credential exist
     */
    @Test
    public void querySnmpCredential_exist() throws UnsupportedEncodingException {

        MvcResult result = UTUtil.performGet("/snmp/credential/1",null);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
        String responseStr = result.getResponse().getContentAsString();
        Map map = JSONObject.parseObject(responseStr, Map.class);
        Object data = map.get("data");
        Assert.assertNotNull(data);
    }

    /**
     * GET /snmp/credential not exist
     */
    @Test
    public void querySnmpCredential_noitexist() throws UnsupportedEncodingException {

        MvcResult result = UTUtil.performGet("/snmp/credential/100",null);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
        String responseStr = result.getResponse().getContentAsString();
        Map map = JSONObject.parseObject(responseStr, Map.class);
        Object data = map.get("data");
        Assert.assertNull(data);
    }

    /**
     * GET /snmp/credential exist
     */
    @Test
    public void querySnmpCredentialList_exist(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/snmp/credential",map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /snmp/credential exist
     */
    @Test
    public void querySnmpCredentialList_notexist(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/snmp/credential",map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * PUT /snmp/credential exist
     */
    @Test
    public void updateSnmpCredential_success(){
        SnmpCredential snmpCredential = jsonToInParameter(testName.getMethodName(), SnmpCredential.class);
        MvcResult result = UTUtil.performPut("/snmp/credential",snmpCredential);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /snmp/credential id is null
     */
    @Test
    public void updateSnmpCredential_idIsNull(){
        SnmpCredential snmpCredential = jsonToInParameter(testName.getMethodName(), SnmpCredential.class);
        MvcResult result = UTUtil.performPut("/snmp/credential",snmpCredential);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_ID_ISNULL);
    }

    /**
     * POST /snmp/credential success
     */
    @Test
    public void addSnmpCredential_success(){
        SnmpCredential snmpCredential = jsonToInParameter(testName.getMethodName(), SnmpCredential.class);
        MvcResult result = UTUtil.performPost("/snmp/credential",snmpCredential);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /snmp/credential name is null
     */
    @Test
    public void addSnmpCredential_nameIsNull(){
        SnmpCredential snmpCredential = jsonToInParameter(testName.getMethodName(), SnmpCredential.class);
        MvcResult result = UTUtil.performPost("/snmp/credential",snmpCredential);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_NAME_ISNULL);
    }

    /**
     * POST /snmp/credential type is null
     */
    @Test
    public void addSnmpCredential_typeIsNull(){
        SnmpCredential snmpCredential = jsonToInParameter(testName.getMethodName(), SnmpCredential.class);
        MvcResult result = UTUtil.performPost("/snmp/credential",snmpCredential);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_TYPE_ISNULL);
    }

    /**
     * POST /snmp/credential config is null
     */
    @Test
    public void addSnmpCredential_configIsNull(){
        SnmpCredential snmpCredential = jsonToInParameter(testName.getMethodName(), SnmpCredential.class);
        MvcResult result = UTUtil.performPost("/snmp/credential",snmpCredential);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_CONFIG_ISNULL);
    }

    /**
     * POST /snmp/credential port is null
     */
    @Test
    public void addSnmpCredential_portIsNull(){
        SnmpCredential snmpCredential = jsonToInParameter(testName.getMethodName(), SnmpCredential.class);
        MvcResult result = UTUtil.performPost("/snmp/credential",snmpCredential);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_PORT_ISNULL);
    }

    /**
     * POST /snmp/credential name duplicate
     */
    @Test
    public void addSnmpCredential_nameDuplicate(){
        SnmpCredential snmpCredential = jsonToInParameter(testName.getMethodName(), SnmpCredential.class);
        MvcResult result = UTUtil.performPost("/snmp/credential",snmpCredential);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_NAME_DUPLICATE);
    }


    /**
     * DELETE /snmp/credential success
     */
    @Test
    public void deleteSnmpCredential_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/snmp/credential",map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /snmp/credential success
     */
    @Test
    public void deleteSnmpCredential_idIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/snmp/credential",map);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_ID_ISNULL);
    }

    /**
     * DELETE /snmp/credential Snmp credential contain assets
     */
    @Test
    public void deleteSnmpCredential_containAssets(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/snmp/credential",map);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_ASSETS_EXISTS);
    }
}
