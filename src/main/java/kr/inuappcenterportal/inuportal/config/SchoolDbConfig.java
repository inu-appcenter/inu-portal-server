package kr.inuappcenterportal.inuportal.config;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

/*@Configuration
public class SchoolDbConfig {
    @Value("${schoolDbUrl}")
    private String url;

    @Value("${schoolDbUser}")
    private String user;

    @Value("${schoolDbPassword}")
    private String password;
    @Bean
    public DataSource dataSource(){
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setInitialSize(1);
        dataSource.setMaxTotal(8);
        dataSource.setMaxIdle(8);
        dataSource.setMinIdle(1);
        dataSource.setMaxWaitMillis(60000);
        dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
        return dataSource;
    }

}*/

