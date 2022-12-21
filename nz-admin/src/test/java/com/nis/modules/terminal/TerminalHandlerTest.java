package com.nis.modules.terminal;

import com.nis.common.utils.RCode;
import com.nis.modules.BaseTest;
import com.nis.modules.UTUtil;
import com.nis.modules.asset.entity.AssetTypeConf;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(SpringRunner.class)
@SpringBootTest
public class TerminalHandlerTest extends BaseTest<AssetTypeConf> {

    @Override
    protected String jsonIndex() {
        return "terminal/TerminalHandlerTestParameter.json";
    }

    /**
     * POST  /terminal/resize success
     */
    @Test()
    public void  resize_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/terminal/resize", map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * POST  /terminal/resize TERMINAL_PARAM_ISNULL
     */
    @Test()
    public void resize_terminalParamIsnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPost("/terminal/resize", map);
        UTUtil.assertEqualsRCode(result, RCode.TERMINAL_PARAM_ISNULL);
    }

    /**
     * Get  /terminal/session success
     */
    @Test()
    public void session_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/terminal/session",map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * Get  /terminal/cmd success
     */
    @Test()
    public void cmd_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/terminal/cmd",map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * Get  /terminal/cmd TERMINAL_UUID_ISNULL
     */
    @Test()
    public void cmd_terminalUuidIsnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/terminal/cmd",map);
        UTUtil.assertEqualsRCode(result, RCode.TERMINAL_UUID_ISNULL);
    }

    /**
     * Get  /terminal/cmd TERMINAL_QUERYSIZE_INVALIDED
     */
    @Test()
    public void cmd_terminalQuerysizeInvalided() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/terminal/cmd",map);
        UTUtil.assertEqualsRCode(result, RCode.TERMINAL_QUERYSIZE_INVALIDED);
    }

    /**
     * Get  /terminal/record success
     */
    @Test()
    public void record_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/terminal/record",map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * Get  /terminal/record TERMINAL_UUID_ISNULL
     */
    @Test()
    public void record_terminalUuidIsnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/terminal/record",map);
        UTUtil.assertEqualsRCode(result, RCode.TERMINAL_UUID_ISNULL);
    }

    /**
     * Get  /terminal/record TERMINAL_QUERYSIZE_INVALIDED
     */
    @Test()
    public void record_terminalQuerysizeInvalided() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performGet("/terminal/record",map);
        UTUtil.assertEqualsRCode(result, RCode.TERMINAL_QUERYSIZE_INVALIDED);
    }

    /**
     * Put  /terminal/kill success
     */
    @Test()
    public void kill_success() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/terminal/kill",map);
        UTUtil.assertEqualsRCode(result, RCode.SUCCESS);
    }

    /**
     * Put  /terminal/kill TERMINAL_UUID_ISNULL
     */
    @Test()
    public void kill_terminalUuidIsnull() {
        Map map = jsonToInParameter(testName.getMethodName(), Map.class);
        MvcResult result = UTUtil.performPut("/terminal/kill",map);
        UTUtil.assertEqualsRCode(result, RCode.TERMINAL_UUID_ISNULL);
    }
}
