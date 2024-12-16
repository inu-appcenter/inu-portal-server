package kr.inuappcenterportal.inuportal.global.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


@Configuration
public class OracleConfig {

    @Value("${schoolDbUrl}")
    private String url;
    @Value("${schoolDbUser}")
    private String username;
    @Value("${schoolDbPassword}")
    private String password;
    @Bean(name = "oracleDataSource")
    @ConfigurationProperties(prefix = "school.datasource")
    public DataSource secondDataSource() {
        /*HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDataSourceClassName("oracle.jdbc.OracleDriver");
        // 커넥션 풀 설정 추가
        config.setMaximumPoolSize(8);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(60000); // milliseconds
        config.setIdleTimeout(600000); // milliseconds
        return new HikariDataSource(config);*/
        return DataSourceBuilder.create().type(HikariDataSource.class).build();
    }

    @Bean(name = "oracleJdbc")
    @Autowired
    public JdbcTemplate jdbcTemplate(@Qualifier("oracleDataSource")DataSource dataSource){
        return new JdbcTemplate(dataSource);
    }
}
