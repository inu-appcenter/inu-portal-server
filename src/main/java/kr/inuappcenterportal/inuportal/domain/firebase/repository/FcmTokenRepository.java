package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken,Long> {
    @Query(value = "SELECT f.token FROM fcm_token f LEFT JOIN member_roles r ON f.member_id = r.member_id WHERE r.roles = 'ROLE_ADMIN'", nativeQuery = true)
    List<String> findAllAdminTokens();
    void deleteByTokenIn(List<String> token);
    boolean existsByToken(String token);
    void deleteAllByToken(String token);
    @Query(value = "SELECT token FROM fcm_token", nativeQuery = true)
    List<String> findAllTokens();
    Optional<FcmToken> findByToken(String token);
    Optional<FcmToken> findByMemberId(Long memberId);
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM fcm_token WHERE create_date < NOW() - INTERVAL 7 DAY", nativeQuery = true)
    void deleteOldTokens();

    @Query("SELECT f FROM FcmToken f WHERE f.memberId IN :memberIds")
    List<FcmToken> findFcmTokensByMemberIds(@Param("memberIds") List<Long> memberIds);
}
