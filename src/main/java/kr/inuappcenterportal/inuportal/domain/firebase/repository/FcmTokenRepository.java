package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken,Long> {
    @Query(value = "SELECT f.token FROM fcm_token f LEFT JOIN member_roles r ON f.member_id = r.member_id WHERE r.roles = 'ROLE_ADMIN'", nativeQuery = true)
    List<String> findAllAdminTokens();
    void deleteByTokenIn(List<String> token);
    boolean existsByToken(String token);
    void deleteAllByToken(String token);
    @Query(value = "SELECT f.token FROM fcm_token f", nativeQuery = true)
    List<String> findAllTokens();
    Optional<FcmToken> findByToken(String token);
    Optional<FcmToken> findByMemberId(Long memberId);
}
