package com.nis.modules;

import cn.hutool.core.io.FileUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSON;
import com.nis.common.utils.Tool;
import org.apache.commons.io.FileUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.cli.CliDocumentation;
import org.springframework.restdocs.http.HttpDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;

@SuppressWarnings("all")
// 默认使用 test 配置进行测试
@ActiveProfiles("test")
@Component
@TestExecutionListeners(mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS, listeners = {DBTestListener.class})
public abstract class BaseTest<T> {

    protected static final Log log = Log.get();

    private static String generateApiName;

    @Value("${nezha.generate.api.name}")
    public void setApiName(String name) {
        generateApiName = Tool.StrUtil.isEmpty(name) ? "Network Zodiac API" : name;
    }

    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    @Rule
    public TestName testName = new TestName();

    @Autowired
    public WebApplicationContext webApplicationContext;

    @Autowired
    org.apache.shiro.mgt.SecurityManager securityManager;

    public static MockMvc mockMvc;

    public RestDocumentationResultHandler documentationHandler;

    // 设置测试类的 Json 文件位置, 继承此类时, 必须要重写此方法
    protected abstract String jsonIndex();

    protected String jsonFile;

    /**
     * 生成单元测试报告
     *
     * https://blog.csdn.net/qq_34379928/article/details/124083432
     */
    @BeforeTest
    public void init() {
        log.info("\njunit test report start\n");
    }
    @AfterTest
    public void generateJUnitReport() {
        log.info("\njunit test report end\n");
    }

    @Before
    public void setUp() {
        jsonFile = "parameters/" + jsonIndex();

        this.documentationHandler = document("{class-name}/{method-name}",
                Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),
                Preprocessors.preprocessResponse(Preprocessors.prettyPrint()));

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.webApplicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(this.restDocumentation)
                        // .snippets().withDefaults(CliDocumentation.curlRequest(), HttpDocumentation.httpRequest(), HttpDocumentation.httpResponse()).and()
                        .uris()
                        .withScheme("http")
                        .withHost("api.network.zodiac.com")
                        .withPort(80)
                )
                .alwaysDo(
                        this.documentationHandler.document(
                                CliDocumentation.curlRequest(),
                                HttpDocumentation.httpRequest(),
                                HttpDocumentation.httpResponse()
                                /*relaxedResponseFields(
                                        fieldWithPath("user.id").description("用户id").optional().type(Number.class),
                                        fieldWithPath("user.name").description("用户姓名").optional().type(String.class),
                                        fieldWithPath("user.username").description("登录用户名").optional().type(String.class)
                                )*/
                        )
                )
                .build();

        // 登录  默认 admin 用户
        SecurityUtils.setSecurityManager(securityManager);
        login("admin", "admin");

        String url = "http://www.baidu.com";



    }

    /**
     * shiro 登录
     *
     * @param username
     * @param pin
     */
    private void login(String username, String pin) {
        UsernamePasswordToken token = new UsernamePasswordToken(username, pin);
        Subject subject = SecurityUtils.getSubject();

        subject.login(token);
    }


    /**
     * 解析resource: parameters/指定JSON文件
     *
     * @param parameterName
     * @param type
     * @param <T>
     * @return
     */
    protected <T> T jsonToInParameter(String parameterName, Class<T> type) {
        Map<String, Object> json = JSON.parseObject(FileUtil.readUtf8String(jsonFile), Map.class);
        Object data = json.get(parameterName);

        String objectJsonStr = JSON.toJSONString(data, true);
        log.info("parameter file: {}, parameter name: {}, \n" +
                "parameter info: \n" +
                "{}", jsonFile, parameterName, objectJsonStr);
        return JSON.parseObject(objectJsonStr, type);
    }

    /**
     * 所有用例执行完之后，生成 Asciidoctor 文件
     * dest: src/test/resources/asciidoc/index.adoc
     * src: /target/generated-snippets/*
     * 语法示例： https://www.baeldung.com/spring-rest-docs
     * https://docs.asciidoctor.org/asciidoc/latest/
     */
    @AfterClass
    public static void generateApiAdocFile() throws Exception {

        log.info("The unit test is completed, starting to generate index.adoc file ");
        try {
            String appDir = System.getProperty("user.dir") + File.separator;
            StringBuilder content = new StringBuilder();
            content.append("= Network Zodiac API Docs\n");
            content.append(":author: shizhendong\n");
            content.append(":email: shizhendong@geedgenetworks.com\n");
            content.append(":revnumber: v1.0\n");
            content.append(":revdate: 2022-02-23\n");
            content.append(":toc: left\n");
            content.append(":toc-title: Table of Contents\n");
            content.append(":toclevels: 4\n");
            content.append(":sectlinks:\n");

            content.append("[[overview]]\n");
            content.append("= Overview\n");
            content.append("[[overview-http-verbs]]\n");
            // HTTP verbs table
            content.append("== HTTP verbs\n");
            content.append("RESTful notes tries to adhere as closely as possible to standard HTTP and REST conventions in its use of HTTP verbs.\n");
            content.append("|===\n");
            content.append("| Verb | Usage\n");
            content.append("| `GET`\n");
            content.append("| Used to retrieve a resource\n");
            content.append("| `POST`\n");
            content.append("| Used to create a new resource\n");
            content.append("| `PATCH`\n");
            content.append("| Used to update an existing resource, including partial updates\n");
            content.append("| `DELETE`\n");
            content.append("| Used to delete an existing resource\n");
            content.append("|===\n");

            content.append("RESTful notes tries to adhere as closely as possible to standard HTTP and REST conventions in its use of HTTP status codes.\n");
            content.append("|===\n");
            content.append("| Status code | Usage\n");
            content.append("| `200 OK`\n");
            content.append("| The request completed successfully\n");
            content.append("| `400 Bad Request`\n");
            content.append("| The request was malformed. The response body will include an error providing further information\n");
            content.append("| `404 Not Found`\n");
            content.append("| The requested resource did not exist\n");
            content.append("| `500 or 999 Internal Server Error\n");
            content.append("| Internal Server Error\n");
            content.append("|===\n");

            String include = "include::";
            File apidirs = new File(appDir + "/target/generated-snippets");
            for (File classDir : apidirs.listFiles()) {
                File[] methods = classDir.listFiles();
                content.append("\n\n");
                // 一级标题  SysUserTest.class 生成出来目录名称为 sys-user-test 驼峰会转成 -，这里处理为 Sys user test
                content.append("== " + Tool.StrUtil.upperFirst(classDir.getName()).replaceAll("-", " ") + "\n");

                List<File> queryMethods = Tool.ListUtil.list(true);
                List<File> saveMethods = Tool.ListUtil.list(true);
                List<File> updateMethods = Tool.ListUtil.list(true);
                List<File> deleteMethods = Tool.ListUtil.list(true);
                List<File> otherMethods = Tool.ListUtil.list(true);
                for (File methodDir : methods) {
                    String name = methodDir.getName();
                    if (name.startsWith("query")) {
                        queryMethods.add(methodDir);
                    } else if (name.startsWith("save")) {
                        saveMethods.add(methodDir);
                    } else if (name.startsWith("update")) {
                        updateMethods.add(methodDir);
                    } else if (name.startsWith("delete")) {
                        deleteMethods.add(methodDir);
                    } else {
                        otherMethods.add(methodDir);
                    }
                }
                if (Tool.CollUtil.isNotEmpty(queryMethods)) {
                    content.append("\n\n");
                    // 二级标题
                    content.append("=== " + "Query" + "\n\n");
                    for (File file : queryMethods) {
                        String mpath = file.getPath();
                        // 三级标题
                        content.append("==== " + restoreMethodName(file.getName()) + "\n");

                        content.append(".CURL-request" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "curl-request.adoc[]", "\n"));

                        content.append(".Request structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-request.adoc[]", "\n"));

                        content.append(".Response structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-response.adoc[]", "\n"));
                    }
                }
                if (Tool.CollUtil.isNotEmpty(saveMethods)) {
                    content.append("\n\n");
                    // 二级标题
                    content.append("=== " + "Save" + "\n\n");
                    for (File file : saveMethods) {
                        String mpath = file.getPath();
                        // 三级标题
                        content.append("==== " + restoreMethodName(file.getName()) + "\n");

                        content.append(".CURL-request" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "curl-request.adoc[]", "\n"));

                        content.append(".Request structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-request.adoc[]", "\n"));

                        content.append(".Response structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-response.adoc[]", "\n"));
                    }
                }
                if (Tool.CollUtil.isNotEmpty(updateMethods)) {
                    content.append("\n\n");
                    // 二级标题
                    content.append("=== " + "Update" + "\n\n");
                    for (File file : updateMethods) {
                        String mpath = file.getPath();
                        // 三级标题
                        content.append("==== " + restoreMethodName(file.getName()) + "\n");

                        content.append(".CURL-request" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "curl-request.adoc[]", "\n"));

                        content.append(".Request structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-request.adoc[]", "\n"));

                        content.append(".Response structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-response.adoc[]", "\n"));
                    }
                }
                if (Tool.CollUtil.isNotEmpty(deleteMethods)) {
                    content.append("\n\n");
                    // 二级标题
                    content.append("=== " + "Delete" + "\n\n");
                    for (File file : deleteMethods) {
                        String mpath = file.getPath();
                        // 三级标题
                        content.append("==== " + restoreMethodName(file.getName()) + "\n");

                        content.append(".CURL-request" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "curl-request.adoc[]", "\n"));

                        content.append(".Request structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-request.adoc[]", "\n"));

                        content.append(".Response structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-response.adoc[]", "\n"));
                    }
                }
                if (Tool.CollUtil.isNotEmpty(otherMethods)) {
                    content.append("\n\n");
                    for (File file : otherMethods) {
                        String mpath = file.getPath();
                        // 三级标题
                        content.append("=== " + restoreMethodName(file.getName()) + "\n");

                        content.append(".CURL-request" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "curl-request.adoc[]", "\n"));

                        content.append(".Request structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-request.adoc[]", "\n"));

                        content.append(".Response structure" + "\n");
                        content.append(String.format("%s%s%s", include, mpath + File.separator + "http-response.adoc[]", "\n"));
                    }
                }
            }

            // 该文件名任意都可 这里命名为 index.adoc
            File file = new File(appDir + "src/test/resources/asciidoc/" + generateApiName + ".adoc");
            FileUtils.writeStringToFile(file, content.toString(), "utf-8");
            log.info("The index.adoc document generation success");
        } catch (IOException e) {
            log.error(e);
            log.error("The index.adoc document generation error");
        }
    }

    /**
     * 还原方法名 输出生成的 adoc 文档
     *
     * @param name 原驼峰会转成 - 例： sysUser -> sys-user 这里转换回来，还原方法名
     * @return
     */
    static String restoreMethodName(String name) {
        char[] chars = name.toCharArray();
        List<Character> characters = new ArrayList<>();
        boolean b = false;
        for (int i = 0; i < chars.length; i++) {
            char aChar = chars[i];
            if (aChar == '-') {
                b = true;
                continue;
            }
            characters.add(b ? Character.toUpperCase(aChar) : aChar);
            b = false;
        }
        return characters.stream().reduce(new StringBuilder(), (sb, s) -> sb.append(s), StringBuilder::append).toString();
    }

}
