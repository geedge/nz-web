package com.nis.modules.monitor;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.module.entity.MonitorModule;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest
public class MonitorModuleTest extends BaseTest<MonitorModule> {

    @Override
    protected String jsonIndex() {
        return "monitor/MonitorModuleTestParameter.json";
    }

    /**
     * GET /monitor/module/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorModuleInfo_ID1() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/module/1", null);

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
     * GET /monitor/module/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorModuleInfo_ID3() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/module/3", null);

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
     * GET /monitor/module/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorModuleInfo_ID4() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/module/12", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object projectInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(projectInfo);

       int starred = (int) ((Map) actualResult.get("data")).get("starred");

       Assert.assertEquals(1,starred);


    }

    /**
     * GET /monitor/module exits
     *
     */
    @Test
    public void queryMonitorModuleList_exits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module", map);

        UTUtil.assertToSizeByFlag(result,true);
    }

    /**
     * GET /monitor/module exits
     *
     */
    @Test
    public void queryMonitorModuleList_starred(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module", map);

        UTUtil.assertToSizeByFlag(result,true);
    }

    /**
     * GET /monitor/module not exits
     *
     */
    @Test
    public void queryMonitorModuleList_notexits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module", map);

        UTUtil.assertToSizeByFlag(result,false);
    }

    /**
     * GET /monitor/module exits
     *
     */
    @Test
    public void queryMonitorModuleTree_exits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module/tree", map);

        UTUtil.assertToSizeByFlag(result,true);
    }

    /**
     * GET /monitor/module not exits
     *
     */
    @Test
    public void queryMonitorModuleTree_notexits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module/tree", map);

        UTUtil.assertToSizeByFlag(result,false);
    }

    /**
     * POST /monitor/module success
     *
     */
    @Test
    public void addMonitorModule_success(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /monitor/module projectId is null
     *
     */
    @Test
    public void addMonitorModule_projectIdIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_PROJECTID_ISNULL);
    }

    /**
     * POST /monitor/module name is null
     *
     */
    @Test
    public void addMonitorModule_nameIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_NAME_ISNULL);
    }

    /**
     * POST /monitor/module endPointNameTmpl Is Null
     *
     */
    @Test
    public void addMonitorModule_endPointNameTmplIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_ENDPOINTNAMETMPL_ISNULL);
    }

    /**
     * POST /monitor/module configs Is Null
     *
     */
    @Test
    public void addMonitorModule_configsIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_CONFIGS_ISNULL);
    }

    /**
     * POST /monitor/module project Not Exits
     *
     */
    @Test
    public void addMonitorModule_projectNotExits(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_NOTFOUND);
    }

    /**
     * POST /monitor/module name is duplicate
     *
     */
    @Test
    public void addMonitorModule_nameDuplicate(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_NAME_DUPLICATE);
    }

    /**
     * POST /monitor/module config type is null
     *
     */
    @Test
    public void addMonitorModule_configTypeIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_TYPE_ISNULL);
    }

    /**
     * POST /monitor/module config type invalid
     *
     */
    @Test
    public void addMonitorModule_configTypeInvalid(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_TYPE_INCONSISTENT);
    }

    /**
     * POST /monitor/module config type duplicate
     *
     */
    @Test
    public void addMonitorModule_configTypeDuplicate(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_CONFIGS_TYPE_DUPLICATE);
    }

    /**
     * POST /monitor/module config Metrics Port Error
     *
     */
    @Test
    public void addMonitorModule_configMetricsPortError(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_PORT_ERROR);
    }

    /**
     * POST /monitor/module config Metrics protocol Error
     *
     */
    @Test
    public void addMonitorModule_configMetricsProtocolError(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_PROTOCOL_INVALID);
    }

    /**
     * POST /monitor/module config Metrics snmpCredentialsId  is null
     *
     */
    @Test
    public void addMonitorModule_configMetricsSnmpCredentialsIdIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_ID_ISNULL);
    }

    /**
     * POST /monitor/module config Metrics snmpCredentials is null
     *
     */
    @Test
    public void addMonitorModule_configMetricsSnmpCredentialsIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.SNMP_CREDENTIAL_NOT_FOUND);
    }

    /**
     * POST /monitor/module config Metrics snmp walk is null
     *
     */
    @Test
    public void addMonitorModule_configMetricsSnmpWalkIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_SNMP_WALK_ISNULL);
    }

    /**
     * POST /monitor/module config logs file is null
     *
     */
    @Test
    public void addMonitorModule_configLogsFileIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPost("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_CONFIG_FILENAME_ISNULL);
    }

    /**
     * PUT /monitor/module success
     *
     */
    @Test
    public void updateMonitorModule_success(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPut("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /monitor/module build in
     *
     */
    @Test
    public void updateMonitorModule_buildIn(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPut("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_BUILDIN_CAN_NOT_REMOVE);
    }

    /**
     * PUT /monitor/module module is null
     *
     */
    @Test
    public void updateMonitorModule_moduleIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPut("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_NOTFOUND);
    }

    /**
     * PUT /monitor/module module id is null
     *
     */
    @Test
    public void updateMonitorModule_moduleIdIsNull(){
        MonitorModule module = jsonToInParameter(testName.getMethodName(), MonitorModule.class);
        MvcResult result = UTUtil.performPut("/monitor/module", module);

        UTUtil.assertEqualsRCode(result, RCode.MODULE_ID_ISNULL);
    }

    /**
     * GET /monitor/module/template success
     *
     */
    @Test
    public void getMonitorModuleTemplate_success() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module/template", map);

        MockHttpServletResponse response = result.getResponse();
        InputStream contentInStream = new ByteArrayInputStream(
                response.getContentAsByteArray());

        Assert.assertEquals("application/excel", response.getContentType());
        Workbook excel = new XSSFWorkbook(contentInStream);
        Sheet datasheet = excel.getSheet("Datasheet");
        Assert.assertNotNull(datasheet);
        int lastRowNum = datasheet.getLastRowNum();
        Assert.assertEquals(0,lastRowNum);

    }

    /**
     * GET /monitor/module/template formatError
     *
     */
    @Test
    public void getMonitorModuleTemplate_formatError(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module/template", map);

        UTUtil.assertEqualsRCode(result,RCode.EXCELFILE_TYPE_ERROR);

    }

    /**
     * GET /monitor/module/template languageError
     *
     */
    @Test
    public void getMonitorModuleTemplate_languageError(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module/template", map);

        UTUtil.assertEqualsRCode(result,RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);

    }

    /**
     * GET /monitor/module/export success
     *
     */
    @Test
    public void getMonitorModuleExportExcel_success() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module/export", map);

        UTUtil.assertToExportFile(result,map,2);

    }

    /**
     * GET /monitor/module/export success
     *
     */
    @Test
    public void getMonitorModuleExportJson_success() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module/export", map);

        UTUtil.assertToExportFile(result,map,2);

    }

    /**
     * GET /monitor/module/export success
     *
     */
    @Test
    public void getMonitorModuleExportCsv_success() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/module/export", map);

        UTUtil.assertToExportFile(result,map,2);

    }

    /**
     * DELETE /monitor/module/cancelImport success
     *
     */
    @Test
    public void MonitorModuleCancelImport_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/module/cancelImport", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);

    }

    /**
     * DELETE /monitor/module success
     *
     */
    @Test
    public void deleteMonitorModule_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/module", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);

    }

}
