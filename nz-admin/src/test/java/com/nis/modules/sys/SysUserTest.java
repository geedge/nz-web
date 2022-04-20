package com.nis.modules.sys;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.sys.entity.SysUserEntity;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
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
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest
public class SysUserTest extends BaseTest<SysUserEntity> {

    @Override
    protected String jsonIndex() {
        return "sys/SysUserTestParameter.json";
    }

    /**
     * POST  /sys/user success
     */
    @Test()
    public void saveUser_success() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPost("/sys/user", sysUserEntity);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST  /sys/user  System user password can not be empty
     */
    @Test()
    public void saveUser_checkPin() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPost("/sys/user", sysUserEntity);
        UTUtil.assertEqualsRCode(result, RCode.SYS_USER_PASSWORD_ISNULL);

    }

    /**
     * POST  /sys/user  username can not be empty
     */
    @Test()
    public void saveUser_checkUsername() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPost("/sys/user", sysUserEntity);

        UTUtil.assertEqualsRCode(result, RCode.SYS_USER_USERNAME_ISNULL);
    }

    /**
     * POST  /sys/user name can not be empty
     */
    @Test()
    public void saveUser_checkName() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPost("/sys/user", sysUserEntity);

        UTUtil.assertEqualsRCode(result, RCode.SYS_USER_NAME_ISNULL);
    }

    /**
     * POST  /sys/user System user name duplicate
     */
    @Test()
    public void saveUser_checkNameDuplicate() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPost("/sys/user", sysUserEntity);

        UTUtil.assertEqualsRCode(result, RCode.SYS_USER_NAME_DUPLICATE);
    }

    /**
     * POST  /sys/user System user status must be 0 or 1
     */
    @Test()
    public void saveUser_checkStatusInvalide() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPost("/sys/user", sysUserEntity);

        UTUtil.assertEqualsRCode(result, RCode.SYS_USER_STATUS_INVALIDE);
    }

    /**
     * POST  /sys/user roleIds is null || role is not exist
     */
    @Test()
    public void saveUser_checkRoleIds() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPost("/sys/user", sysUserEntity);

        UTUtil.assertNotEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /sys/user success
     */
    @Test()
    public void updateUser_success() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPut("/sys/user", sysUserEntity);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT  /sys/user System user name duplicate
     */
    @Test()
    public void updateUser_checkNameDuplicate() {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPut("/sys/user", sysUserEntity);

        UTUtil.assertEqualsRCode(result, RCode.SYS_USER_NAME_DUPLICATE);
    }

    /**
     * GET /sys/user/id user exist
     *
     * @throws Exception
     */
    @Test
    public void queryUserInfo_exist() throws Exception {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performGet(String.format("/sys/user/%s", sysUserEntity.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object userInfo = actualResult.get("user");

        // isExist
        Assert.assertNotNull(userInfo);
    }

    /**
     * GET /sys/user/id user not exist
     *
     * @throws Exception
     */
    @Test
    public void queryUserInfo_notexist() throws Exception {
        SysUserEntity sysUserEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performGet(String.format("/sys/user/%s", sysUserEntity.getId()), null);

        String responseStr = result.getResponse().getContentAsString();
        Assert.assertNotNull("No success response", responseStr);

        Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
        Object userInfo = actualResult.get("user");

        // isExist
        Assert.assertNull(userInfo);
    }


    /**
     * GET /sys/user exist
     */
    @Test
    public void queryUserList_exist() {
        Map<String, Object> queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/sys/user", queryParam);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /sys/user not exist
     */
    @Test
    public void queryUserList_notexist() {
        Map queryParam = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/sys/user", queryParam);

        UTUtil.assertToSizeByFlag(result, false);
    }


    /**
     * PUT /sys/user/pin success
     */
    @Test
    public void updatePin_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/sys/user/pin", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }


    /**
     * PUT /sys/user/pin oldPin is null
     */
    @Test
    public void updatePin_checkoldPin() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/sys/user/pin", map);

        UTUtil.assertNotEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /sys/user/pin newPin is null
     */
    @Test
    public void updatePin_checknewPin() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/sys/user/pin", map);

        UTUtil.assertNotEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /sys/user/pin newPin is error
     */
    @Test
    public void updatePin_oldPinError() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/sys/user/pin", map);

        UTUtil.assertNotEqualsRCode(result, RCode.SUCCESS);
    }


    /**
     * GET /sys/user/disableMfa
     */
    @Test
    public void disableMfa() {
        MvcResult result = UTUtil.performGet("/sys/user/disableMfa", null);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }


    /**
     * GET /sys/user/resetMfa SUCCESS
     */
    @Test
    public void resetMfa_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/user/resetMfa", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }


    /**
     * DELEET /sys/user success
     */
    @Test
    public void deleteUser_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/user", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELEET /sys/user del admin error
     */
    @Test
    public void deleteUser_adminNotDel() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/user", map);

        UTUtil.assertEqualsRCode(result, RCode.SYS_USER_DELADMIN);
    }

    /**
     * GET /sys/user/profile success
     */
    @Test
    public void queryUserProfile_success() throws UnsupportedEncodingException {
        MvcResult result = UTUtil.performGet("/sys/user/profile", null);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);

        String contentAsString = result.getResponse().getContentAsString();
        Map<String,Object> resultMap = JSONObject.parseObject(contentAsString, Map.class);
        Object data = resultMap.get("user");
        Assert.assertNotNull(data);
    }

    /**
     * PUT /sys/user/profile success
     */
    @Test
    public void updateUserProfile_success(){
        SysUserEntity userEntity = jsonToInParameter(testName.getMethodName(), SysUserEntity.class);
        MvcResult result = UTUtil.performPut("/sys/user/profile", userEntity);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);

    }
}
