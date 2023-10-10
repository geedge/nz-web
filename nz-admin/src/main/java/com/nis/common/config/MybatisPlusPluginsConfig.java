package com.nis.common.config;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IKeyGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.nis.common.exception.NZException;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RCode;
import com.nis.common.utils.ToolUtil;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * mybatis-plus 插件配置
 */
@Configuration
@DependsOn(value = {"flywayConfig"})
public class MybatisPlusPluginsConfig {

    private static final Log logger = Log.get();

    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    public IKeyGenerator seqKeyGenerator() {
        return new SeqKeyGenerator();
    }

    /**
     * 自定义主键生成器
     *
     * @author fang
     */
    static class SeqKeyGenerator implements IKeyGenerator {
        @Autowired
        DatabaseIdProvider databaseIdProvider;
        @Autowired
        DataSource dataSource;

        @Override
        public String executeSql(String incrementerName) {
            String databaseId = null;
            try {
                databaseId = databaseIdProvider.getDatabaseId(dataSource);
            } catch (SQLException e) {
                logger.error(e);
            }

            if (ToolUtil.isEmpty(databaseId)) {
                throw new NZException(RCode.ERROR);
            }

            switch (databaseId) {
                case Constant.MYSQL:
                    return String.format("select seq_nextval('%s')", incrementerName);
                case Constant.POSTGRESQL:
                    return String.format("select seq_nextval('%s')", incrementerName);
                case Constant.ORACLE:
                    return String.format("select %s.nextval from dual", incrementerName);
                default:
                    return String.format("select seq_nextval('%s')", incrementerName);
            }
        }

        @Override
        public DbType dbType() {
            String databaseId = null;
            try {
                databaseId = databaseIdProvider.getDatabaseId(dataSource);
            } catch (SQLException e) {
                logger.error(e);
            }

            if (ToolUtil.isEmpty(databaseId)) {
                throw new NZException(RCode.ERROR);
            }

            switch (databaseId) {
                case Constant.MYSQL:
                    return DbType.MYSQL;
                case Constant.POSTGRESQL:
                    return DbType.POSTGRE_SQL;
                case Constant.ORACLE:
                    return DbType.ORACLE;
                default:
                    return DbType.MYSQL;
            }
        }
    }

}
