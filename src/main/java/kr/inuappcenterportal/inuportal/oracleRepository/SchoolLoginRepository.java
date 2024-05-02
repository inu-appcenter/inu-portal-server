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

    @Autowired
    @Qualifier("oracleJdbc")
    private JdbcTemplate jdbcTemplate;


    public boolean loginCheck(String username, String password) {
        String sql = "SELECT F_LOGIN_CHECK(?, ?) FROM DUAL";
        log.info("학교 로그인 조회 :{}",username);
        try {
            String result = jdbcTemplate.queryForObject(sql, String.class, username, password);
            log.info("학교 디비 조회 결과 : {}",result);
            return "Y".equals(result);
        } catch (Exception e) {
            log.info("데이터베이스 연결 오류 메시지 : {}",e.getMessage());
            return false;
        }
    }
}
