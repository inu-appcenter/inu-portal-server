package kr.inuappcenterportal.inuportal.global.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "app.local-auth.enabled", havingValue = "false", matchIfMissing = true)
public class OracleConfig {

    @Value("${school.datasource.jdbc-url}")
    private String url;
    @Value("${school.datasource.username}")
    private String username;
    @Value("${school.datasource.password}")
    private String password;
    @Value("${school.datasource.driver-class-name}")
    private String driverClassName;

    @Bean(name = "oracleDataSource")
    public DataSource secondDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);

        dataSource.setMaximumPoolSize(8);
        dataSource.setMinimumIdle(1);
        dataSource.setConnectionTimeout(60000);

        return dataSource;
    }

    @Bean(name = "oracleJdbc")
    public JdbcTemplate jdbcTemplate(@Qualifier("oracleDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}