package com.nis.modules.sys;

import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.sys.entity.SysMenuEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

/**
 * @FixMethodOrder You should never write tests that need to be executed in a specified order. That's really bad practice. Every test should be able to run independent.
 * 执行特定顺序对于单元测试来说是一种不好的做法，但对于集成测试来说不一定是一种不好的做法。
 * 最好的选择（不理想）是用注释类 @FixMethodOrder(MethodSorters.NAME_ASCENDING)，执行顺序按字母顺序执行
 * 根据需要可命名 例如 t1_firstTest()，t2_secondTest()
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SysMenuTest extends BaseTest<SysMenuEntity> {

    @Override
    protected String jsonIndex() {
        return "sys/SysMenuTestParameter.json";
    }

    /**
     * GET /sys/menu exist
     */
    @Test
    public void queryMenuList_exist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/sys/menu", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /sys/menu exist
     */
    @Test
    public void queryMenuListByIdIsNull_exist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/sys/menu", map);

        UTUtil.assertToSizeByFlag(result, true);
    }

    /**
     * GET /sys/menu not exist
     */
    @Test
    public void queryMenuList_notexist() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/sys/menu", map);

        UTUtil.assertToSizeByFlag(result, false);
    }

    /**
     * POST /sys/menu success
     */
    @Test
    public void addMenu_success() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST /sys/menu name is null
     */
    @Test
    public void addMenu_nameIsNull() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_NAME_ISNULL);
    }

    /**
     * POST /sys/menu code is null
     */
    @Test
    public void addMenu_codeIsNull() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_CODE_ISNULL);
    }

    /**
     * POST /sys/menu i18n is null
     */
    @Test
    public void addMenu_i18nIsNull() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_I18N_ISNULL);
    }

    /**
     * POST /sys/menu type is null
     */
    @Test
    public void addMenu_typeIsNull() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_TYPE_ISNULL);
    }

    /**
     * POST /sys/menu orderNum is null
     */
    @Test
    public void addMenu_orderNumeIsNull() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_ORDERNUM_ISNULL);
    }

    /**
     * POST /sys/menu code duplicate
     */
    @Test
    public void addMenu_codeDuplicate() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_CODE_DUPLICATE);
    }

    /**
     * POST /sys/menu type error
     */
    @Test
    public void addMenu_typeError() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_TYPE_INVALIDE);
    }

    /**
     * POST /sys/menu menu is null
     */
    @Test
    public void addMenu_menuIsNull() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_ISNULL);
    }

    /**
     * POST /sys/menu menu type is Button
     */
    @Test
    public void addMenu_menuTypeIsButton() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_BUTTON_PARENTTYPE_INVALIDE);
    }

    /**
     * POST /sys/menu menu type is Table
     */
    @Test
    public void addMenu_menuTypeIsTable() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPost("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_TAB_PARENTTYPE_INVALIDE);
    }

    /**
     * PUT /sys/menu success
     */
    @Test
    public void updateMenu_success() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPut("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * PUT /sys/menu id is null
     */
    @Test
    public void updateMenu_idIsNull() {
        SysMenuEntity menu = jsonToInParameter(testName.getMethodName(), SysMenuEntity.class);
        MvcResult result = UTUtil.performPut("/sys/menu", menu);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_ID_ISNULL);
    }

    /**
     * DELETE /sys/menu success
     */
    @Test
    public void deleteMenu_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/menu", map);

        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * DELETE /sys/menu id is null
     */
    @Test
    public void deleteMenu_idIsNull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/menu", map);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_ID_ISNULL);
    }

    /**
     * DELETE /sys/menu menu disable
     */
    @Test
    public void deleteMenu_menuDisable() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performDelete("/sys/menu", map);

        UTUtil.assertEqualsRCode(result, RCode.SYS_MENU_DELSUB);
    }
}
