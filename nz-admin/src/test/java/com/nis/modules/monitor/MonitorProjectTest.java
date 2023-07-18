package com.nis.modules.monitor;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.project.entity.MonitorProject;
import com.nis.modules.project.entity.Topo;
import com.nis.modules.project.entity.TopoIcon;
import com.qiniu.util.IOUtils;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest
public class MonitorProjectTest extends BaseTest<MonitorProject> {

    @Override
    protected String jsonIndex() {
        return "monitor/MonitorProjectTestParameter.json";
    }

    /**
     * GET /monitor/project/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorProjectInfo_ID1() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/project/1", null);

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
     * GET /monitor/project/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorProjectInfo_ID2() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/project/2", null);

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
        Assert.assertNotEquals("0", p2Alert);

        String p3Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P3", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertEquals("0", p3Alert);
    }


    /**
     * GET /monitor/project/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorProjectInfo_ID3() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/project/3", null);

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
        Assert.assertNotEquals("0", p2Alert);

        String p3Alert = alert.stream().filter(map -> Tool.StrUtil.equals("P3", map.get("priority").toString())).findFirst().orElseThrow(RuntimeException::new).get("num").toString();
        Assert.assertEquals("0", p3Alert);
    }

    /**
     * GET /monitor/project/id
     *
     * @throws Exception
     */
    @Test
    public void queryMonitorProjectInfo_ID4() throws Exception {
        MvcResult result = UTUtil.performGet("/monitor/project/7", null);

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
     * GET /monitor/project exist
     *
     */
    @Test
    public void queryMonitorProjectList_exist(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/project", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /monitor/project exist
     *
     */
    @Test
    public void queryMonitorProjectList_starred(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/project", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /monitor/project not exist
     *
     */
    @Test
    public void queryMonitorProjectList_notexist(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/project", map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * POST /monitor/project  success
     *
     */
    @Test
    public void addMonitorProject_success() {
        MonitorProject project = jsonToInParameter(testName.getMethodName(), MonitorProject.class);
        MvcResult result = UTUtil.performPost("/monitor/project", project);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /monitor/project  name is null
     *
     */
    @Test
    public void addMonitorProject_nameisnull() {
        MonitorProject project = jsonToInParameter(testName.getMethodName(), MonitorProject.class);
        MvcResult result = UTUtil.performPost("/monitor/project", project);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_NAME_ISNULL);
    }

    /**
     * POST /monitor/project  name is duplicate
     *
     */
    @Test
    public void addMonitorProject_nameisduplicate() {
        MonitorProject project = jsonToInParameter(testName.getMethodName(), MonitorProject.class);
        MvcResult result = UTUtil.performPost("/monitor/project", project);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_NAME_DUPLICATE);
    }

    /**
     * PUT /monitor/project  success
     *
     */
    @Test
    public void updateMonitorProject_success() {
        MonitorProject project = jsonToInParameter(testName.getMethodName(), MonitorProject.class);
        MvcResult result = UTUtil.performPut("/monitor/project", project);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /monitor/project  id is null
     *
     */
    @Test
    public void updateMonitorProject_idisnull() {
        MonitorProject project = jsonToInParameter(testName.getMethodName(), MonitorProject.class);
        MvcResult result = UTUtil.performPut("/monitor/project", project);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_ID_ISNULL);
    }

    /**
     * PUT /monitor/project  built-in project does not allow editing
     *
     */
    @Test
    public void updateMonitorProject_builtIn() {
        MonitorProject project = jsonToInParameter(testName.getMethodName(), MonitorProject.class);
        MvcResult result = UTUtil.performPut("/monitor/project", project);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_BUILDIN_CAN_NOT_EDIT);
    }

    /**
     * DELETE /monitor/project  success
     *
     */
    @Test
    public void deleteMonitorProject_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/project", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /monitor/project  id is null
     *
     */
    @Test
    public void deleteMonitorProject_idisnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/project", map);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_ID_ISNULL);
    }

    /**
     * DELETE /monitor/project  id is null
     *
     */
    @Test
    public void deleteMonitorProject_starred() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/project", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * GET /monitor/project/topo/iconUnit  success
     *
     */
    @Test
    public void queryTopoIconUnit_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/project/topo/iconUnit", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /monitor/project/topo/iconUnit  success
     *
     */
    @Test
    public void deleteTopoIconUnit_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/project/topo/iconUnit", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /monitor/project/topo/iconUnit  unit isnull
     *
     */
    @Test
    public void deleteTopoIconUnit_unitsisnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/project/topo/iconUnit", map);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_TOPOICON_UNIT_ISNULL);
    }


    /**
     * GET /monitor/project/topo/icon  exits
     *
     */
    @Test
    public void queryTopoIconList_exits() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/project/topo/icon", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /monitor/project/topo/icon  not exits
     *
     */
    @Test
    public void queryTopoIconList_notexits() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/project/topo/icon", map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * DELETE /monitor/project/topo/icon  success
     *
     */
    @Test
    public void deleteTopoIcon_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/project/topo/icon", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /monitor/project/topo/icon  ids is null
     *
     */
    @Test
    public void deleteTopoIcon_idsisnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/monitor/project/topo/icon", map);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_ID_ISNULL);
    }


    /**
     * GET /project/topo/icon/{id}/{base64}  success
     *
     * @throws Exception
     */
    @Test
    public void downloadIconFile_successbasein0() throws IOException {
        MvcResult result = UTUtil.performGet("/monitor/project/topo/icon/1/0", null);

        String contentType = result.getResponse().getContentType();
        Assert.assertEquals("image/png",contentType);

        InputStream contentInStream = new ByteArrayInputStream(
                result.getResponse().getContentAsByteArray());
        byte[] bytes = IOUtils.toByteArray(contentInStream);

        Assert.assertNotNull(bytes);
    }

    /**
     * GET /project/topo/icon/{id}/{base64}  success
     *
     * @throws Exception
     */
    @Test
    public void downloadIconFile_successbasein1() throws IOException {
        MvcResult result = UTUtil.performGet("/monitor/project/topo/icon/1/1", null);

        String contentType = result.getResponse().getContentType();
        Assert.assertEquals("text/plain",contentType);

        InputStream contentInStream = new ByteArrayInputStream(
                result.getResponse().getContentAsByteArray());
        byte[] bytes = IOUtils.toByteArray(contentInStream);

        Assert.assertNotNull(bytes);
    }

    /**
     * GET /project/topo/icon/{id}/{base64}  icon is null
     *
     */
    @Test
    public void downloadIconFile_iconisnull() {
        MvcResult result = UTUtil.performGet("/monitor/project/topo/icon/10/0", null);

        UTUtil.assertEqualsRCode(result,RCode.PROJECT_ICON_NOTFOUND);
    }

    /**
     * GET /project/topo/icon/{id}/{base64}  base64 error
     *
     */
    @Test
    public void downloadIconFile_baseerror(){
        MvcResult result = UTUtil.performGet("/monitor/project/topo/icon/1/2", null);

        UTUtil.assertEqualsRCode(result,RCode.PROJECT_ICON_BASE64_VALUEERROR);
    }

    /**
     * GET /monitor/project/topo exits
     *
     */
    @Test
    public void queryProjectTopo_exits() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/project/topo", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * GET /monitor/project/topo projectId isnull
     *
     */
    @Test
    public void queryProjectTopo_prijectIdisnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/monitor/project/topo", map);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_ID_ISNULL);
    }

    /**
     * PUT /monitor/project/topo success
     *
     */
    @Test
    public void updateProjectTopo_success() {
        Topo map = jsonToInParameter(testName.getMethodName(), Topo.class);
        MvcResult result = UTUtil.performPut("/monitor/project/topo", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /monitor/project/topo projectId is null
     *
     */
    @Test
    public void updateProjectTopo_projectIdisnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/project/topo", map);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_ID_ISNULL);
    }

    /**
     * PUT /monitor/project/topo topo is null
     *
     */
    @Test
    public void updateProjectTopo_topoisnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/project/topo", map);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_TOPOCONFIG_ISNULL);
    }

    /**
     * PUT /monitor/project/topo project is null
     *
     */
    @Test
    public void updateProjectTopo_projectisnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/monitor/project/topo", map);

        UTUtil.assertEqualsRCode(result, RCode.PROJECT_NOTFOUND);
    }
}
