package com.nis.modules.asset;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.asset.entity.Cabinet;
import com.nis.modules.asset.entity.Dc;
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
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest
public class DcTest extends BaseTest<Dc> {

    @Override
    protected String jsonIndex() {
        return "asset/DcTestParameter.json";
    }

    /**
     * GET /dc/id
     *
     * @throws Exception
     */
    @Test
    public void queryDcInfo_ID1() throws Exception {
        MvcResult result = UTUtil.performGet("/dc/1", null);

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
     * GET /dc/id
     *
     * @throws Exception
     */
    @Test
    public void queryDcInfo_ID2() throws Exception {
        MvcResult result = UTUtil.performGet("/dc/2", null);

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
     * GET /dc dc exist
     */
    @Test
    public void queryDcList_exist() {
        Map queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /dc dc not exist
     */
    @Test
    public void queryDcList_notexist() {
        Map queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc", queryParam);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * POST /dc success
     */
    @Test
    public void addDc_success() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPost("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /dc name is null
     */
    @Test
    public void addDc_nameIsNull() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPost("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.DC_NAME_ISNULL);
    }

    /**
     * POST /dc state is null
     */
    @Test
    public void addDc_stateIsNull() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPost("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.DC_STATE_ISNULL);
    }

    /**
     * POST /dc principal is null
     */
    @Test
    public void addDc_principalIsNull() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPost("/dc", dc);
        UTUtil.assertEqualsRCode(result, RCode.DC_PRINCIPAL_NOT_EXISTS);
    }

    /**
     * POST /dc name DUPLICATE
     */
    @Test
    public void addDc_nameDUPLICATE() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPost("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.DC_NAME_DUPLICATE);
    }

    /**
     * POST /dc state error
     */
    @Test
    public void addDc_stateError() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPost("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.DC_STATE_ERROR);
    }

    /**
     * POST /dc longitude error
     */
    @Test
    public void addDc_longitudeError() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPost("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.DC_LONGITUDE_ERROR);
    }

    /**
     * POST /dc latitude error
     */
    @Test
    public void addDc_latitudeError() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPost("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.DC_LATITUDE_ERROR);
    }

    /**
     * PUT /dc success
     */
    @Test
    public void updateDc_success() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPut("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /dc id is null
     */
    @Test
    public void updateDc_idIsNull() {
        Dc dc = jsonToInParameter(testName.getMethodName(), Dc.class);
        MvcResult result = UTUtil.performPut("/dc", dc);

        UTUtil.assertEqualsRCode(result, RCode.DC_ID_ISNULL);
    }

    /**
     * DELETE /dc success
     */
    @Test
    public void DeleteDc_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/dc", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /dc id is null
     */
    @Test
    public void DeleteDc_idIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/dc", map);

        UTUtil.assertEqualsRCode(result, RCode.DC_ID_ISNULL);
    }

    /**
     * GET /dc/template language is zh
     *
     */
    @Test
    public void getDcTemplate_zh() throws IOException {
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
     * GET /dc/template language is en
     *
     */
    @Test
    public void getDcTemplate_en() throws IOException {
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
     * GET /dc/export success
     *
     */
    @Test
    public void getDcExport_excel() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * GET /dc/export success
     *
     */
    @Test
    public void getDcExport_csv() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * GET /dc/export success
     *
     */
    @Test
    public void getDcExport_json() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * DELETE /dc/cancelImport success
     *
     */
    @Test
    public void cancelDcExport_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/dc/cancelImport", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * DELETE /dc/cancelImport  seq is null
     *
     */
    @Test
    public void cancelDcExport_seqIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/dc/cancelImport", map);

        UTUtil.assertEqualsRCode(result,RCode.UNDO_IMPORT_SEQ_ISNULL);
    }


    /**
     * GET /dc/cabinet/id  exist
     *
     */
    @Test
    public void queryCabinetInfo_exist() throws UnsupportedEncodingException {
        MvcResult result = UTUtil.performGet("/dc/cabinet/1", null);

        String resultString = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", resultString);

        Map<String,Object> resultMap = JSONObject.parseObject(resultString, Map.class);
        Object data = resultMap.get("data");
        Assert.assertNotNull(data);
    }

    /**
     * GET /dc/cabinet/id  cabinet not exist
     *
     */
    @Test
    public void queryCabinetInfo_notexist() {
        MvcResult result = UTUtil.performGet("/dc/cabinet/20", null);

        UTUtil.assertEqualsRCode(result,RCode.CABINET_NOT_EXISTS);
    }


    /**
     * GET /dc/cabinet  exist
     *
     */
    @Test
    public void queryCabinetList_exist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet", map);

        UTUtil.assertToSizeByFlag(result,true);
    }

    /**
     * GET /dc/cabinet  not exist
     *
     */
    @Test
    public void queryCabinetList_notexist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet", map);

        UTUtil.assertToSizeByFlag(result,false);
    }

    /**
     * POST /dc/cabinet  success
     *
     */
    @Test
    public void addCabinet_success() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPost("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * POST /dc/cabinet  name is null
     *
     */
    @Test
    public void addCabinet_nameIsNull() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPost("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.DC_CABINETNAME_ISNULL);
    }

    /**
     * POST /dc/cabinet  dcId is null
     *
     */
    @Test
    public void addCabinet_dcIdIsNull() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPost("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.DC_ID_ISNULL);
    }

    /**
     * POST /dc/cabinet  cabinet y error
     *
     */
    @Test
    public void addCabinet_yError() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPost("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.CABINET_Y_MINIMUM);
    }

    /**
     * POST /dc/cabinet  cabinet x error
     *
     */
    @Test
    public void addCabinet_xError() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPost("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.CABINET_X_MINIMUM);
    }

    /**
     * POST /dc/cabinet  dc is null
     *
     */
    @Test
    public void addCabinet_dcIsNull() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPost("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.DC_NOT_EXISTS);
    }

    /**
     * POST /dc/cabinet  name duplicate
     *
     */
    @Test
    public void addCabinet_nameDuplicate() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPost("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.DC_CABINETNAME_DUPLICATE);
    }

    /**
     * POST /dc/cabinet  usize error
     *
     */
    @Test
    public void addCabinet_usizeError() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPost("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.DC_CABINET_MAXU);
    }

    /**
     * PUT /dc/cabinet  success
     *
     */
    @Test
    public void updateCabinet_success() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPut("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * PUT /dc/cabinet  id is null
     *
     */
    @Test
    public void updateCabinet_idIsNull() {
        Cabinet cabinet = jsonToInParameter(testName.getMethodName(), Cabinet.class);
        MvcResult result = UTUtil.performPut("/dc/cabinet", cabinet);

        UTUtil.assertEqualsRCode(result,RCode.DC_CABINETID_ISNULL);
    }

    /**
     * DELETE /dc/cabinet  success
     *
     */
    @Test
    public void deleteCabinet_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/dc/cabinet", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * DELETE /dc/cabinet  ids is null
     *
     */
    @Test
    public void deleteCabinet_idsIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/dc/cabinet", map);

        UTUtil.assertEqualsRCode(result,RCode.DC_CABINETID_ISNULL);
    }

    /**
     * GET /dc/cabinet/u  exist
     *
     */
    @Test
    public void queryCabinetU_exist() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/u", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);

        String responsesJsonStr = result.getResponse().getContentAsString();
        Map<String,Object> resultMap = JSONObject.parseObject(responsesJsonStr, Map.class);
        Map<String,Object> dataMap = (Map<String, Object>) resultMap.get("data");
        List occupy = (List) dataMap.get("occupy");

        Assert.assertNotEquals(occupy.size(),0);
    }

    /**
     * GET /dc/cabinet/u  cabinet id is null
     *
     */
    @Test
    public void queryCabinetU_cabinetIdIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/u", map);

        UTUtil.assertEqualsRCode(result,RCode.DC_CABINETID_ISNULL);

    }

    /**
     * GET /dc/cabinet/u  cabinet is null
     *
     */
    @Test
    public void queryCabinetU_cabinetIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/u", map);

            UTUtil.assertEqualsRCode(result,RCode.CABINET_NOT_EXISTS);

    }

    /**
     * GET /dc/cabinet/template language is zh
     *
     */
    @Test
    public void getCabinetTemplate_zh() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/template", map);

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
     * GET /dc/cabinet/template language is en
     *
     */
    @Test
    public void getCabinetTemplate_en() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/template", map);

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
     * GET /dc/cabinet/template language error
     *
     */
    @Test
    public void getCabinetTemplate_languageError(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/template", map);

        UTUtil.assertEqualsRCode(result,RCode.EXCELFILE_HEADER_LANGUAGE_ERROR);
    }

    /**
     * GET /dc/cabinet/template format error
     *
     */
    @Test
    public void getCabinetTemplate_formatError(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/template", map);

        UTUtil.assertEqualsRCode(result,RCode.EXCELFILE_TYPE_ERROR);
    }

    /**
     * GET /dc/cabinet/export success
     *
     */
    @Test
    public void getCabinetExport_excel() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * GET /dc/cabinet/export success
     *
     */
    @Test
    public void getCabinetExport_csv() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * GET /dc/cabinet/export success
     *
     */
    @Test
    public void getCabinetExport_json() throws IOException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/dc/cabinet/export", map);

        UTUtil.assertToExportFile(result,map,2);
    }

    /**
     * DELETE /dc/cabinet/cancelImport success
     *
     */
    @Test
    public void cancelCabinetExport_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/dc/cabinet/cancelImport", map);

        UTUtil.assertEqualsRCode(result,RCode.SUCCESS);
    }

    /**
     * DELETE /dc/cabinet/cancelImport  seq is null
     *
     */
    @Test
    public void cancelCabinetExport_seqIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/dc/cabinet/cancelImport", map);

        UTUtil.assertEqualsRCode(result,RCode.UNDO_IMPORT_SEQ_ISNULL);
    }

}
