package com.nis.modules.monitor;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.endpoint.entity.MonitorEndpoint;
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
public class MonitorEndpointTest extends BaseTest<MonitorEndpoint> {

    @Override
    protected String jsonIndex() {
        return "monitor/MonitorEndpointTestParameter.json";
    }


    /**
     * GET /monitor/endpoint/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorEndpointInfo_ID1() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/endpoint/1", null);

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
     * GET /monitor/endpoint/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorEndpointInfo_ID2() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/endpoint/2", null);

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
     * GET /monitor/endpoint exits
     *
     */
    @Test
    public void queryMonitorEndpointList_exits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint", map);

        UTUtil.assertToSizeByFlag(result,true);
    }


    /**
     * GET /monitor/endpoint not exits
     *
     */
    @Test
    public void queryMonitorEndpointList_notExits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint", map);

        UTUtil.assertToSizeByFlag(result,false);
    }

    /**
     * GET /monitor/endpoint/tree exits
     *
     */
    @Test
    public void queryMonitorEndpointTree_exits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint/tree", map);

        UTUtil.assertToSizeByFlag(result,true);
    }

    /**
     * GET /monitor/endpoint/tree not exits
     *
     */
    @Test
    public void queryMonitorEndpointTree_notExits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint/tree", map);

        UTUtil.assertToSizeByFlag(result,false);
    }

    /**
     * POST /monitor/endpoint success
     *
     */
    @Test
    public void addMonitorEndpoint_success(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * POST /monitor/endpoint name is null
     *
     */
    @Test
    public void addMonitorEndpoint_nameIsNull(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_NAME_ISNULL);
    }

    /**
     * POST /monitor/endpoint assetId is null
     *
     */
    @Test
    public void addMonitorEndpoint_assetIdIsNull(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_ASSETID_ISNULL);
    }

    /**
     * POST /monitor/endpoint moduleId is null
     *
     */
    @Test
    public void addMonitorEndpoint_moduleIdIsNull(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_MODULEID_ISNULL);
    }

    /**
     * POST /monitor/endpoint asset not exits
     *
     */
    @Test
    public void addMonitorEndpoint_assetNotExits(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.ASSET_NOT_EXIST);
    }

    /**
     * POST /monitor/endpoint module not exits
     *
     */
    @Test
    public void addMonitorEndpoint_moduleNotExits(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.MODULE_NOTFOUND);
    }

    /**
     * POST /monitor/endpoint name duplicate
     *
     */
    @Test
    public void addMonitorEndpoint_nameDuplicate(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_NAME_DUPLICATE);
    }

    /**
     * PUT /monitor/endpoint success
     *
     */
    @Test
    public void updateMonitorEndpoint_success(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * PUT /monitor/endpoint id is null
     *
     */
    @Test
    public void updateMonitorEndpoint_idIsNull(){
        MonitorEndpoint endpoint = jsonToInParameter(testName.getMethodName(), MonitorEndpoint.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint", endpoint);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_ID_ISNULL);
    }

    /**
     * DELETE /monitor/endpoint success
     *
     */
    @Test
    public void deleteMonitorEndpoint_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/endpoint", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * DELETE /monitor/endpoint id is null
     *
     */
    @Test
    public void deleteMonitorEndpoint_idIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/endpoint", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_ID_ISNULL);
    }

    /**
     * GET /monitor/endpoint/template language is cn
     *
     */
    @Test
    public void getMonitorEndpointTemplate_en() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint/template", map);

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
     * GET /monitor/endpoint/template language is zh
     *
     */
    @Test
    public void getMonitorEndpointTemplate_zh() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint/template", map);

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
     * GET /monitor/endpoint/template language error
     *
     */
    @Test
    public void getMonitorEndpointTemplate_languageError() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint/template", map);

        UTUtil.assertEqualsRCode(result,RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
    }

    /**
     * DELETE /monitor/endpoint/cancelImport success
     *
     */
    @Test
    public void deleteMonitorEndpointCancelImport_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/endpoint/cancelImport", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * DELETE /monitor/endpoint/cancelImport seq is null
     *
     */
    @Test
    public void deleteMonitorEndpointCancelImport_seqIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/endpoint/cancelImport", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_CANCELIMPORTSEQ_ISNULL);
    }

    /**
     * GET /monitor/endpoint/export success
     *
     */
    @Test
    public void getMonitorEndpointExport_excel() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * GET /monitor/endpoint/export success
     *
     */
    @Test
    public void getMonitorEndpointExport_csv() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * GET /monitor/endpoint/export success
     *
     */
    @Test
    public void getMonitorEndpointExport_json() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/endpoint/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * POST /monitor/endpoint/render success
     *
     */
    @Test
    public void getMonitorEndpointRender_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint/render", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * POSTmonitor/endpoint/render asset exits
     *
     */
    @Test
    public void getMonitorEndpointRender_assetExits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint/render", map);

        UTUtil.assertEqualsRCode(result,RCode.ASSET_NOT_EXIST);
    }

    /**
     * POST /monitor/endpoint/render module Exits
     *
     */
    @Test
    public void getMonitorEndpointRender_moduleExits(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint/render", map);

        UTUtil.assertEqualsRCode(result,RCode.MODULE_NOTFOUND);
    }

    /**
     * POST /monitor/endpoint/bulk success
     *
     */
    @Test
    public void addMonitorEndpointBulk_success(){
        List list = jsonToInParameter(testName.getMethodName(), List.class);
        MvcResult result = UTUtil.performPost("/monitor/endpoint/bulk", list);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * PUT /monitor/endpoint/bulk success
     * editType 1
     */
    @Test
    public void putEndpointBulk1_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * PUT /monitor/endpoint/bulk success
     * editType 2
     */
    @Test
    public void putEndpointBulk2_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * PUT /monitor/endpoint/bulk success
     * editType 3
     */
    @Test
    public void putEndpointBulk3_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * PUT /monitor/endpoint/bulk ENDPOINT_EDITTYPE_ISNULL
     */
    @Test
    public void putEndpointBulk_endpointEdittypeIsnull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_EDITTYPE_ISNULL);
    }

    /**
     * PUT /monitor/endpoint/bulk ENDPOINT_ID_ISNULL
     */
    @Test
    public void putEndpointBulk_endpointIdIsnull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_ID_ISNULL);
    }

    /**
     * PUT /monitor/endpoint/bulk ENDPOINT_LABELS_ISNULL
     */
    @Test
    public void putEndpointBulk_endpointLabelsIsnull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_LABELS_ISNULL);
    }

    /**
     * PUT /monitor/endpoint/bulk ENDPOINT_LABELS_ACTION_ISNULL
     */
    @Test
    public void putEndpointBulk_endpointLabelsActionIsnull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_LABELS_ACTION_ISNULL);
    }

    /**
     * PUT /monitor/endpoint/bulk ENDPOINT_LABELS_KEY_ISNULL
     */
    @Test
    public void putEndpointBulk_endpointLabelsKeyIsnull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_LABELS_KEY_ISNULL);
    }

    /**
     * PUT /monitor/endpoint/bulk ENDPOINT_LABELS_ACTION_INVALIDE
     */
    @Test
    public void putEndpointBulk_endpointLabelsActionInvalide(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_LABELS_ACTION_INVALIDE);
    }

    /**
     * PUT /monitor/endpoint/bulk ENDPOINT_ENABLED_INVALIDE
     */
    @Test
    public void putEndpointBulk_endpointEnabledInvalide(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/endpoint/bulk", map);

        UTUtil.assertEqualsRCode(result,RCode.ENDPOINT_ENABLED_INVALIDE);
    }

}
