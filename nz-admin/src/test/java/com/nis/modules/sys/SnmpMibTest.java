package com.nis.modules.sys;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.sys.entity.SnmpMib;
import net.percederberg.mibble.Mib;
import net.percederberg.mibble.MibLoader;
import net.percederberg.mibble.MibLoaderException;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.*;
import java.util.Map;
import java.util.UUID;

/**
 * @FixMethodOrder You should never write tests that need to be executed in a specified order. That's really bad practice. Every test should be able to run independent.
 * 执行特定顺序对于单元测试来说是一种不好的做法，但对于集成测试来说不一定是一种不好的做法。
 * 最好的选择（不理想）是用注释类 @FixMethodOrder(MethodSorters.NAME_ASCENDING)，执行顺序按字母顺序执行
 * 根据需要可命名 例如 t1_firstTest()，t2_secondTest()
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SnmpMibTest extends BaseTest<SnmpMib> {


    @Override
    protected String jsonIndex() {
        return "sys/SnmpMibTestParameter.json";
    }

    /**
     * GET /mib exist
     */
    @Test
    public void querySnmpMiblList_exist(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/mib",map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /mib not exist
     */
    @Test
    public void querySnmpMiblList_notexist(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/mib",map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * POST /mib success
     */
    @Test
    public void addSnmpMibl_success(){
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performMultipart("/mib","/file/CESHI-MIB.txt","file",snmpMib,null);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /mib success
     */
    @Test
    public void updateSnmpMibl_success(){
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performMultipart("/mib","/file/CESHI-MIB.txt","file",snmpMib,null);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /mib id is null
     */
    @Test
    public void updateSnmpMibl_idIsNull(){
        SnmpMib snmpMib = jsonToInParameter(testName.getMethodName(), SnmpMib.class);
        MvcResult result = UTUtil.performPut("/mib",snmpMib);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_MIB_ID_ISNULL);
    }

    /**
     * DELETE /mib id is null
     */
    @Test
    public void deleteSnmpMibl_success(){
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/mib",snmpMib);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /mib id is null
     */
    @Test
    public void deleteSnmpMibl_idIsNull(){
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/mib",snmpMib);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_MIB_ID_ISNULL);
    }

    /**
     * GET /mib/download success
     */
    @Test
    public void getSnmpMibl_text() throws IOException, MibLoaderException {
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/mib/download",snmpMib);

        InputStream contentInStream = new ByteArrayInputStream(
                result.getResponse().getContentAsByteArray());

        String contentType = result.getResponse().getContentType();
        Assert.assertEquals("multipart/form-data",contentType);

        MibLoader ml =new MibLoader();
        File mibFile = new File(Constant.TEMP_PATH + File.separator + "tempMibFile" + UUID.randomUUID());
        FileUtils.copyInputStreamToFile(contentInStream, mibFile);
        Mib mib= ml.load(mibFile);

        Assert.assertNotNull(mib);
    }

    /**
     * GET /mib/download id is null
     */
    @Test
    public void getSnmpMibl_idIsNull(){
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/mib/download",snmpMib);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_MIB_ID_ISNULL);
    }

    /**
     * GET /mib/download mib not exist
     */
    @Test
    public void getSnmpMibl_mibNotExist() throws UnsupportedEncodingException {
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/mib/download",snmpMib);

        Assert.assertEquals("snmp mib cannot be found.",result.getResponse().getContentAsString());
    }

    /**
     * GET /mib/tree success
     */
    @Test
    public void getSnmpMiblTree_success() throws UnsupportedEncodingException {
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/mib/tree",snmpMib);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);

        String responsrStr = result.getResponse().getContentAsString();
        Map map = JSONObject.parseObject(responsrStr, Map.class);
        Object data = map.get("data");
        Assert.assertNotNull(data);
    }

    /**
     * POST /mib/browser success
     */
    @Test
    public void getSnmpMiblBrowser_success(){
        Map snmpMib = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/mib/browser",snmpMib);


        UTUtil.assertToSizeByFlag(result, true);
    }


}
