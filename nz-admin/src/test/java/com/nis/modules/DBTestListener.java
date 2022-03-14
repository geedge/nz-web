package com.nis.modules;

import cn.hutool.log.Log;
import com.nis.common.utils.Tool;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;
import java.io.File;
import java.nio.charset.StandardCharsets;

public class DBTestListener extends AbstractTestExecutionListener {

    protected static final Log log = Log.get();

    @Autowired
    private DataSource dataSource;

    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        testContext.getApplicationContext()
                .getAutowireCapableBeanFactory()
                .autowireBean(this);

        ScriptRunner runner = new ScriptRunner(dataSource.getConnection());
        // 设置不自动提交
        // runner.setAutoCommit(false);
        // true，遇见错误会停止执行，打印并抛出异常，捕捉异常，并进行回滚，保证在一个事务内执行；
        // false，遇见错误不会停止，会继续执行，会打印异常信息，并不会抛出异常，当前方法无法捕捉异常无法进行回滚操作，无法保证在一个事务内执行；
        runner.setStopOnError(true);
        // false则按照自定义的分隔符每行执行；true则获取整个脚本并执行；
        runner.setSendFullScript(false);
        // 定义命令间的分隔符
        runner.setDelimiter(";");
        runner.setFullLineDelimiter(false);
        // 设置是否输出日志，null不输出日志，不设置自动将日志输出到控制台
        runner.setLogWriter(null);

        File dbdirs = new File("src/test/resources/db");
        for (File file : dbdirs.listFiles()) {
            try {
                log.info("sql file: {}, sql name: {}", file, file.getName());
                runner.runScript(Tool.FileUtil.getReader(file, StandardCharsets.UTF_8));
                log.info("{} exec success", file.getName());
            } catch (Exception e) {
                log.error("sql file: {} exec error", file.getName());
                log.error(e);
            }
        }
    }
}
