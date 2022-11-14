package com.nis.modules;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.util.CharsetUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.Tool;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Assert;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("all")
public class UTUtil extends BaseTest {

    static final String Authorization = "Authorization";
    static final String TEST_TOKEN = "nezha";

    @Override
    protected String jsonIndex() {
        return null;
    }

    public static MvcResult performPost(String url, Object requestBody) {
        MvcResult result = null;
        try {
            result = mockMvc.perform(
                    MockMvcRequestBuilders.post(url)
                            .header(Authorization, TEST_TOKEN)
                            .accept(MediaType.APPLICATION_JSON_UTF8)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONObject.toJSONString(requestBody))
            )
                    // .andDo(MockMvcResultHandlers.print())
                    // .andExpect(MockMvcResultMatchers.status().isOk())
                    .andReturn();
        } catch (Exception e) {
            log.error(e);
        }
        return result;
    }

    public static MvcResult performPut(String url, Object requestBody) {
        MvcResult result = null;
        try {
            result = mockMvc.perform(
                    MockMvcRequestBuilders.put(url)
                            .header(Authorization, TEST_TOKEN)
                            .accept(MediaType.APPLICATION_JSON_UTF8)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JSONObject.toJSONString(requestBody))
            )
                    //.andDo(MockMvcResultHandlers.print())
                    .andReturn();
        } catch (Exception e) {
            log.error(e);
        }
        return result;
    }

    public static MvcResult performGet(String url, Map<String, Object> queryParam) {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get(url)
                .header(Authorization, TEST_TOKEN)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.APPLICATION_JSON);

        if (Tool.ObjectUtil.isNotEmpty(queryParam)) {
            for (Map.Entry<String, Object> entry : queryParam.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value != null && value instanceof Map) {
                    //增加请求头参数
                    requestBuilder.param(key, JSONObject.toJSONString(value))
                    .header(key, JSONObject.toJSONString(value));
                }
                //增加请求头参数
                requestBuilder.param(key, value == null ? null : value.toString())
                        .header(key, value == null ? null : value.toString());
            }
        }

        MvcResult result = null;
        try {
            result = mockMvc.perform(requestBuilder)
                    //.andDo(MockMvcResultHandlers.print())
                    .andReturn();
        } catch (Exception e) {
            log.error(e);
        }
        return result;
    }


    public static MvcResult performDelete(String url, Map<String, Object> queryParam) {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.delete(url)
                .header(Authorization, TEST_TOKEN)
                .accept(MediaType.APPLICATION_JSON_UTF8)
                .contentType(MediaType.APPLICATION_JSON);

        if (Tool.ObjectUtil.isNotEmpty(queryParam)) {
            for (Map.Entry<String, Object> entry : queryParam.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value != null && value instanceof Map) {
                    requestBuilder.param(key, JSONObject.toJSONString(value));
                }
                requestBuilder.param(key, value == null ? null : value.toString());
            }
        }

        MvcResult result = null;
        try {
            result = mockMvc.perform(requestBuilder)
                    //.andDo(MockMvcResultHandlers.print())
                    .andReturn();
        } catch (Exception e) {
            log.error(e);
        }
        return result;
    }

    public static MvcResult performMultipart(String url, String path, String name, Map<String, Object> queryParam, Map<String, String> requestHeader) {
        MvcResult result = null;
        try {
            File file = FileUtil.file("parameters/" + path);
            MockMultipartFile mockMultipartFile = new MockMultipartFile(name, file.getName(), null, FileUtil.getInputStream(file));

            MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.multipart(url)
                    .file(mockMultipartFile)
                    .header(Authorization, TEST_TOKEN);

            if (Tool.ObjectUtil.isNotEmpty(queryParam)) {
                for (Map.Entry<String, Object> entry : queryParam.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null && value instanceof Map) {
                        requestBuilder.param(key, JSONObject.toJSONString(value));
                    }
                    requestBuilder.param(key, value == null ? null : value.toString());
                }
            }

            if (Tool.ObjectUtil.isNotEmpty(requestHeader)) {
                for (Map.Entry<String, String> entry : requestHeader.entrySet()) {
                    requestBuilder.header(entry.getKey(), entry.getValue());
                }
            }

            result = mockMvc.perform(
                    requestBuilder
            ).andReturn();
        } catch (Exception e) {
            log.error(e);
        }
        return result;
    }


    /**
     * 用于断言接口实际返回 Rcode 是否和 期望Rcode 一致
     *
     * @param result     实际返回结果
     * @param expectedOk 期望 Rcode
     */
    public static void assertEqualsRCode(MvcResult result, RCode expectedRcode) {
        try {
            String responseStr = result.getResponse().getContentAsString();
            Assert.assertNotNull("No success response", responseStr);

            Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
            // assertEquals
            Assert.assertEquals(expectedRcode.getCode(), actualResult.get("code"));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }
    }

    /**
     * 用于断言接口实际返回 Rcode 是否和 期望Rcode 一致
     *
     * @param result     实际返回结果
     * @param expectedOk 期望 Rcode
     */
    public static void assertNotEqualsRCode(MvcResult result, RCode expectedRcode) {
        try {
            String responseStr = result.getResponse().getContentAsString();
            Assert.assertNotNull("No success response", responseStr);

            Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
            // assertNotEquals
            Assert.assertNotEquals(expectedRcode.getCode(), actualResult.get("code"));
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }
    }

    /**
     * 用于 GET 列表方法 断言 size
     *
     * @param result
     * @param expectedOk 期望请求接口是否成功
     */
    public static void assertToSizeByFlag(MvcResult result, Boolean expectedExist) {
        try {
            String responseStr = result.getResponse().getContentAsString();
            Assert.assertNotNull("No success response", responseStr);

            Map<String, Object> actualResult = JSONObject.parseObject(responseStr, Map.class);
            Map data = (Map) actualResult.get("data");

            JSONArray list = (JSONArray) data.get("list");
            if (expectedExist) {
                // isExist
                Assert.assertNotEquals(0, list.size());
            } else {
                // is not Exist
                Assert.assertEquals(0, list.size());
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }
    }

    /**
     * 用于  断言文件导出
     *
     * @param result
     * @param map 请求参数
     * @param row 期望请求 返回正确条数
     */
    public static void assertToExportFile(MvcResult result, Map map,int row) throws IOException {
        File tempCsvFile = null;
        try {
            String responseStr = result.getResponse().getContentAsString();
            Assert.assertNotNull("No success response", responseStr);

            MockHttpServletResponse response = result.getResponse();
            InputStream contentInStream = new ByteArrayInputStream(
                    response.getContentAsByteArray());
            Integer format = (Integer) map.get("format") == null ? 1 : (Integer) map.get("format");
            switch (format) {
                case 1: { // XLSX
                    //断言格式
                    Assert.assertEquals("application/excel", response.getContentType());
                    Workbook excel = new XSSFWorkbook(contentInStream);
                    Sheet datasheet = excel.getSheet("Datasheet");
                    Assert.assertNotNull(datasheet);
                    int lastRowNum = datasheet.getLastRowNum();
                    Assert.assertEquals(row, lastRowNum);
                    break;
                }
                case 2: {// CSV
                    tempCsvFile = new File(Constant.TEMP_PATH + File.separator + "csv_" + UUID.randomUUID());
                    FileUtils.copyInputStreamToFile(contentInStream, tempCsvFile);
                    CsvData data = Tool.CsvUtil.getReader().read(ResourceUtil.getReader(tempCsvFile.getPath(), CharsetUtil.CHARSET_GBK));
                    Assert.assertNotNull(data);
                    List<CsvRow> rows = data.getRows();
                    Assert.assertEquals(row+1, rows.size());
                    break;
                }
                case 3: {// JSON
                    String jsonStr = IoUtil.read(contentInStream, CharsetUtil.charset("UTF-8"));
                    Assert.assertNotNull(jsonStr);
                    List<LinkedHashMap> mapList = JSONObject.parseArray(jsonStr, LinkedHashMap.class);
                    Assert.assertEquals(row, mapList.size());
                    break;
                }
                default: {
                    break;
                }
            }

        } catch (Exception e) {
            log.error(e);
        } finally {
            if (tempCsvFile != null && tempCsvFile.exists()) tempCsvFile.delete();
        }
    }
}
