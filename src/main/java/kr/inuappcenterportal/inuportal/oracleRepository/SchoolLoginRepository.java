package kr.inuappcenterportal.inuportal.oracleRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.sql.*;

@Component
@Slf4j
public class SchoolLoginRepository {

    /*@Autowired
    @Qualifier("oracleJdbc")
    private JdbcTemplate jdbcTemplate;


    public boolean loginCheck(String username, String password) {
        String sql = "SELECT F_LOGIN_CHECK(?, ?) FROM DUAL";
        try {
            String result = jdbcTemplate.queryForObject(sql, String.class, username, password);
            return "1".equals(result);
        } catch (Exception e) {
            log.info("데이터베이스 연결 오류 메시지 : {}",e.getMessage());
            return false;
        }
    }*/
}
