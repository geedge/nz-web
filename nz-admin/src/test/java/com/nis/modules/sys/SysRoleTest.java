package com.nis.modules.sys;

import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.sys.entity.SysRoleEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;


/**
 * @FixMethodOrder You should never write tests that need to be executed in a specified order. That's really bad practice. Every test should be able to run independent.
 * 执行特定顺序对于单元测试来说是一种不好的做法，但对于集成测试来说不一定是一种不好的做法。
 * 最好的选择（不理想）是用注释类 @FixMethodOrder(MethodSorters.NAME_ASCENDING)，执行顺序按字母顺序执行
 * 根据需要可命名 例如 t1_firstTest()，t2_secondTest()
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SysRoleTest extends BaseTest<SysRoleEntity> {

    @Override
    protected String jsonIndex() {
        return "sys/SysRoleTestParameter.json";
    }

    /**
     * GET /sys/role exist
     */
    @Test
    public void queryRole_exist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/sys/role", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /sys/role notexist
     */
    @Test
    public void queryRole_notexist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/sys/role", map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * GET /sys/role/menu/{id} exits
     */
    @Test
    public void queryRoleMenu_exist() throws UnsupportedEncodingException {

        MvcResult result = UTUtil.performGet("/sys/role/menu/1", null);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);

        String responseJsonStr = result.getResponse().getContentAsString();
        Map<String, Object> resultMap = JSONObject.parseObject(responseJsonStr, Map.class);
        Map<String,List> dataMap = (Map<String, List>) resultMap.get("data");

        List menus = dataMap.get("menus");
        List selectedIds = dataMap.get("selectedIds");

        Assert.assertNotEquals(menus.size(),0);
        Assert.assertNotEquals(selectedIds.size(),0);
    }

    /**
     * GET /sys/role/menu/{id} notexits
     */
    @Test
    public void queryRoleMenu_notexist() {

        MvcResult result = UTUtil.performGet("/sys/role/menu/10", null);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_ISNULL);
    }

    /**
     * POST /sys/role success
     */
    @Test
    public void addRole_success() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPost("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /sys/role name is null
     */
    @Test
    public void addRole_nameIsNull() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPost("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_NAME_ISNULL);
    }

    /**
     * POST /sys/role name  duplicate
     */
    @Test
    public void addRole_nameDuplicate() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPost("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_NAME_DUPLICATE);
    }

    /**
     * POST /sys/role menuIds  is null
     */
    @Test
    public void addRole_menuIdsIsNull() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPost("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_MENUID_ISNULL);
    }

    /**
     * POST /sys/role menu  is null
     */
    @Test
    public void addRole_menuIsNull() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPost("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_ISNULL);
    }

    /**
     * PUT /sys/role success
     */
    @Test
    public void updateRole_success() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPut("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /sys/role id is null
     */
    @Test
    public void updateRole_idIsNull() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPut("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_ID_ISNULL);
    }

    /**
     * PUT /sys/role role not exist
     */
    @Test
    public void updateRole_roleNotExist() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPut("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_ISNULL);
    }

    /**
     * PUT /sys/role built-in
     */
    @Test
    public void updateRole_builtIn() {
        SysRoleEntity role = jsonToInParameter(testName.getMethodName(), SysRoleEntity.class);
        MvcResult result = UTUtil.performPut("/sys/role", role);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_NOT_SUPPORT_MODIFY);
    }

    /**
     * DELETE /sys/role success
     */
    @Test
    public void deleteRole_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/role", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /sys/role id is null
     */
    @Test
    public void deleteRole_idIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/role", map);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_ID_ISNULL);
    }

    /**
     * DELETE /sys/role built-in
     */
    @Test
    public void deleteRole_builtIn() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/role", map);

        UTUtil.assertEqualsRCode(result, RCode.SYS_ROLE_CANNOT_DELETE);
    }

}
