/**

 *

 *

 */

package com.nis.common.config;

import com.nis.common.utils.Constant;
import com.nis.common.utils.SQLUtils;
import com.nis.common.utils.SpringContextUtils;
import com.nis.common.utils.SqlHelper.MysqlHelper;
import com.nis.common.utils.SqlHelper.OracleHelper;
import com.nis.common.utils.SqlHelper.PostgresqlHelper;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.VendorDatabaseIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * WebMvc配置
 *

 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /*@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/statics/**").addResourceLocations("classpath:/statics/");
    }*/
    @Bean
    public DatabaseIdProvider getDatabaseIdProvider(){
        DatabaseIdProvider databaseIdProvider = new VendorDatabaseIdProvider();
        Properties properties = new Properties();
        properties.setProperty("Oracle","oracle");
        properties.setProperty("MySQL","mysql");
        properties.setProperty("DB2","db2");
        properties.setProperty("Derby","derby");
        properties.setProperty("H2","h2");
        properties.setProperty("HSQL","hsql");
        properties.setProperty("Informix","informix");
        properties.setProperty("MS-SQL","ms-sql");
        properties.setProperty("PostgreSQL","postgresql");
        properties.setProperty("Sybase","sybase");
        properties.setProperty("Hana","hana");
        databaseIdProvider.setProperties(properties);

        return databaseIdProvider;
    }
    @Bean
    public SQLUtils getSqlUtils() throws SQLException {
        switch (Constant.DB_TYPE){
            case Constant.MYSQL:
                return new MysqlHelper();
            case Constant.POSTGRESQL:
                return new PostgresqlHelper();
            case Constant.ORACLE:
                return new OracleHelper();
            default:
                return new MysqlHelper();
        }
    }
}