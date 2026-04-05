package kr.inuappcenterportal.inuportal.domain.member.repository;

import kr.inuappcenterportal.inuportal.global.config.LocalAuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class SchoolLoginRepository {

    @Qualifier("oracleJdbc")
    private final ObjectProvider<JdbcTemplate> oracleJdbcProvider;
    private final LocalAuthProperties localAuthProperties;

    public boolean loginCheck(String username, String password) {
        if (localAuthProperties.isEnabled()) {
            return localAuthProperties.findSeedUser(username)
                    .map(seedUser -> seedUser.getPassword() != null && seedUser.getPassword().equals(password))
                    .orElse(false);
        }

        String sql = "SELECT F_LOGIN_CHECK(?,?) FROM DUAL";
        log.info("school login request. studentId:{}", username);
        try {
            JdbcTemplate jdbcTemplate = oracleJdbcProvider.getIfAvailable();
            if (jdbcTemplate == null) {
                log.warn("oracleJdbc is not configured. studentId:{}", username);
                return false;
            }

            String result = jdbcTemplate.queryForObject(sql, String.class, username, password);
            log.info("school login result. studentId:{}, result:{}", username, result);
            return "Y".equals(result);
        } catch (Exception e) {
            log.info("school login database error message : {}", e.getMessage());
            return false;
        }
    }

    public List<String> resolveRoles(String studentId) {
        if (!localAuthProperties.isEnabled()) {
            return List.of("ROLE_USER");
        }

        return localAuthProperties.findSeedUser(studentId)
                .map(LocalAuthProperties.SeedUser::getResolvedRoles)
                .orElse(List.of("ROLE_USER"));
    }
}
