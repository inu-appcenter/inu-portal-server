package kr.inuappcenterportal.inuportal.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.local-auth")
public class LocalAuthProperties {

    private boolean enabled = false;

    private List<SeedUser> seedUsers = new ArrayList<>();

    public Optional<SeedUser> findSeedUser(String studentId) {
        return seedUsers.stream()
                .filter(seedUser -> seedUser.getStudentId() != null)
                .filter(seedUser -> seedUser.getStudentId().equals(studentId))
                .findFirst();
    }

    @Getter
    @Setter
    public static class SeedUser {
        private String studentId;
        private String password;
        private List<String> roles = new ArrayList<>();

        public List<String> getResolvedRoles() {
            if (roles == null || roles.isEmpty()) {
                return List.of("ROLE_USER");
            }
            return List.copyOf(roles);
        }
    }
}
