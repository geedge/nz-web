package com.nis.modules.asset;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.asset.entity.IssueRecord;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;


@RunWith(SpringRunner.class)
@SpringBootTest
public class IssueRecordTest extends BaseTest<IssueRecord> {

    @Override
    protected String jsonIndex() {
        return "asset/IssueRecordTestParameter.json";
    }

    /**
     * GET /issue/{id} exist
     */
    @Test
    public void queryIssueRecordById() throws UnsupportedEncodingException {
        MvcResult result = UTUtil.performGet("/issue/1", null);
        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object projectInfo = actualResult.get("data");

        // isExist
        Assert.assertNotNull(projectInfo);
    }

    /**
     * GET /issue success
     */
    @Test
    public void queryIssueRecordList_success() throws UnsupportedEncodingException {
        MvcResult result = UTUtil.performGet("/issue", null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Map resultInfo = (Map)actualResult.get("data");
        Assert.assertNotNull(resultInfo);

        Object list = resultInfo.get("list");
        Assert.assertNotNull(list);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * GET /issue success
     */
    @Test
    public void queryIssueRecordListByParams_success() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/issue", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Map resultInfo = (Map)actualResult.get("data");
        Assert.assertNotNull(resultInfo);

        Object list = resultInfo.get("list");
        Assert.assertNotNull(list);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * GET /issue   not exist
     */
    @Test
    public void queryIssueRecordListByParams_notExist() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/issue", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Map resultInfo = (Map)actualResult.get("data");
        Assert.assertNotNull(resultInfo);

        List list = (List)resultInfo.get("list");
        Assert.assertEquals(0, list.size());

    }

    /**
     * GET /issue/type   success
     */
    @Test
    public void queryIssueRecordType() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/issue/type", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Map resultInfo = (Map)actualResult.get("data");
        Assert.assertNotNull(resultInfo);

        List list = (List)resultInfo.get("list");
        Assert.assertNotEquals(0, list.size());
    }

    /**
     * POST /issue success
     */
    @Test
    public void addIssueRecord_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /issue ISSUE_NAME_ISNULL
     */
    @Test
    public void addIssueRecord_nameIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_NAME_ISNULL);
    }

    /**
     * POST /issue ISSUE_TYPE_ISNULL
     */
    @Test
    public void addIssueRecord_typeIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_TYPE_ISNULL);
    }

    /**
     *  POST /issue ISSUE_CONTENT_ISNULL
     */
    @Test
    public void addIssueRecord_contentIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_CONTENT_ISNULL);
    }

    /**
     * POST /issue ISSUE_STATE_ISNULL
     */
    @Test
    public void addIssueRecord_stateIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_STATE_ISNULL);
    }

    /**
     * POST /issue ISSUE_RID_ID_ISNULL
     */
    @Test
    public void addIssueRecord_ridIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_RID_ID_ISNULL);
    }

    /**
     * POST /issue ISSUE_AID_ID_ISNULL
     */
    @Test
    public void addIssueRecord_aidIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_AID_ID_ISNULL);
    }

    /**
     * POST /issue ISSUE_NAME_DUPLICATE
     */
    @Test
    public void addIssueRecord_name_duplicate() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_NAME_DUPLICATE);
    }

    /**
     * POST /issue ISSUE_STATE_INVALID
     */
    @Test
    public void addIssueRecord_stateIsError() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_STATE_INVALID);
    }

    /**
     * POST /issue ISSUE_PRIORITY_INVALID
     */
    @Test
    public void addIssueRecord_priorityIsError() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_PRIORITY_INVALID);
    }

    /**
     * PUT /issue success
     */
    @Test
    public void updateIssueRecord_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /issue ISSUE_ID_ISNULL
     */
    @Test
    public void updateIssueRecord_idIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ID_ISNULL);
    }


    /**
     * DELETE /issue success
     */
    @Test
    public void deleteIssueRecord_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/issue", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }


    /**
     * GET /issue/activity success
     */
    @Test
    public void queryIssueActivityListByParams_success() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/issue/activity", map);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Map resultInfo = (Map)actualResult.get("data");
        Assert.assertNotNull(resultInfo);

        Object list = resultInfo.get("list");
        Assert.assertNotNull(list);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * GET /issue/activity ISSUE_ID_ISNULL
     */
    @Test
    public void queryIssueActivityListByParams_issueIdIsNull() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ID_ISNULL);
    }

    /**
     * GET /issue/activity ISSUE_ACTIVITY_TYPE_ISNULL
     */
    @Test
    public void queryIssueActivityListByParams_typeIsNull() throws UnsupportedEncodingException {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_TYPE_ISNULL);

    }

    /**
     * POST /issue/activity success
     */
    @Test
    public void addIssueActivity_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /issue/activity ISSUE_ID_ISNULL
     */
    @Test
    public void addIssueActivity_issueIdIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ID_ISNULL);
    }

    /**
     * POST /issue/activity ISSUE_ACTIVITY_CONTENT_ISNULL
     */
    @Test
    public void addIssueActivity_contentIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_CONTENT_ISNULL);
    }

    /**
     * PUT /issue/activity success
     */
    @Test
    public void updateIssueActivity_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /issue/activity ISSUE_ACTIVITY_ID_ISNULL
     */
    @Test
    public void updateIssueActivity_idIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_ID_ISNULL);
    }

    /**
     * PUT /issue/activity ISSUE_ACTIVITY_CONTENT_ISNULL
     */
    @Test
    public void updateIssueActivity_contentIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_CONTENT_ISNULL);
    }

    /**
     * PUT /issue/activity ISSUE_ACTIVITY_ONLYOWN_COMMENTS
     */
    @Test
    public void updateIssueActivity_notOwn(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_ONLYOWN_COMMENTS);
    }

    /**
     * PUT /issue/activity ISSUE_ACTIVITY_ONLYOWN_COMMENTS
     */
    @Test
    public void updateIssueActivity_notComment(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_ONLYOWN_COMMENTS);
    }

    /**
     * DELETE /issue/activity success
     */
    @Test
    public void deleteIssueActivity_success(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /issue/activity ISSUE_ACTIVITY_ID_ISNULL
     */
    @Test
    public void deleteIssueActivity_idIsNull(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_ID_ISNULL);
    }

    /**
     * DELETE /issue/activity ISSUE_ACTIVITY_ONLYOWN_COMMENTS
     */
    @Test
    public void deleteIssueActivity_notOwn(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_ONLYOWN_COMMENTS);
    }

    /**
     * DELETE /issue/activity ISSUE_ACTIVITY_ONLYOWN_COMMENTS
     */
    @Test
    public void deleteIssueActivity_notComment(){
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/issue/activity", map);
        UTUtil.assertEqualsRCode(result, RCode.ISSUE_ACTIVITY_ONLYOWN_COMMENTS);
    }
}
