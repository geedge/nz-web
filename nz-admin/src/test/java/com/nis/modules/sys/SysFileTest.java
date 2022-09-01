package com.nis.modules.sys;

import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.sys.entity.SysFileEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SysFileTest extends BaseTest<SysFileEntity> {


    @Override
    protected String jsonIndex() {
        return "sys/SysFileTestParameter.json";
    }

    /**
     * GET /file success
     */
    @Test
    public void querySysFilel_success(){
        MvcResult result = UTUtil.performGet("/file/1",null);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * GET /file exist
     */
    @Test
    public void querySysFilelList_exist() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/file",map);
        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /file not exist
     */
    @Test
    public void querySysFilelList_notexist(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/file",map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * POST /file/upload success
     */
    @Test
    public void addSysFilel_success(){
        MvcResult result = UTUtil.performMultipart("/file/upload","/file/CESHI-MIB.txt","file",null,null);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /file/download SYS_FILE_NOTEXIST
     */
    @Test
    public void downSysFilel_notexist(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/file/download/uuid10",map);
        UTUtil.assertEqualsRCode(result, RCode.SYS_FILE_NOTEXIST);
    }

    /**
     * DELETE /file SYS_FILE_ID_ISNULL
     */
    @Test
    public void delSysFilel_idIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/file",map);
        UTUtil.assertEqualsRCode(result, RCode.SYS_FILE_ID_ISNULL);
    }

    /**
     * DELETE /file success
     */
    @Test
    public void delSysFilel_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/file",map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

}
