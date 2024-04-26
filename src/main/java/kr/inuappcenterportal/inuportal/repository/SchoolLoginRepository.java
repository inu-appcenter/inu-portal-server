package kr.inuappcenterportal.inuportal.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.sql.*;

@Repository
@Slf4j
public class SchoolLoginRepository {
    @Value("${schoolDbUrl}")
    private String url;

    @Value("${schoolDbUser}")
    private String username;
    @Value("${schoolDbPassword}")
    private String password;
    public Connection connectJdbc() throws SQLException, ClassNotFoundException {
        log.info("데이터베이스 연결");
        Class.forName("oracle.jdbc.OracleDriver");
        try {
            return DriverManager.getConnection(url
                    , username
                    , password);
        }
        catch (SQLException e){
            log.info("데이터베이스 연결 실패 메시지 : {}",e.getMessage());
            return null;
        }
    }

    public boolean loginCheck(String username, String password) throws SQLException, ClassNotFoundException {
        log.info("메소드 실행");
        Connection connection = connectJdbc();
        if(connection==null){
            return false;
        }
        String sql = "SELECT F_LOGIN_CHECK(?, ?) FROM DUAL";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            ResultSet resultSet = preparedStatement.executeQuery();
            log.info("{}",resultSet.getString(1));
            return true;
        } catch (SQLException e) {
            log.info("데이터 베이스 연결 실패2 메시지 :{} ",e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
