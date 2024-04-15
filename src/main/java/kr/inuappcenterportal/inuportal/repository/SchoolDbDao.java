package kr.inuappcenterportal.inuportal.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/*@Repository
@RequiredArgsConstructor
public class SchoolDbDao {
    private final JdbcTemplate jdbcTemplate;

    public boolean loginCheck(String id, String password){
        String sql = "SELECT F_LOGIN_CHECK(?, ?) FROM DUAL";
        return jdbcTemplate.queryForObject(sql, Boolean.class, id, password);
    }
}*/
