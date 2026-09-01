package kr.inuappcenterportal.inuportal.firebase;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 유실 보정 스케줄러가 의존하는 경계 쿼리들이 정확한 하한/상한을 지키는지 검증한다.
 * 여기서 검증하는 경계가 #431 사고(레거시 backfill 잔재의 재발송)를 구조적으로 막는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class FcmMessageRepositoryTest {

    @Autowired
    FcmMessageRepository fcmMessageRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    @DisplayName("findStalledCandidates는 notBefore 이전에 생성된 행을 절대 반환하지 않는다")
    void findStalledCandidatesExcludesRowsBeforeNotBefore() {
        LocalDateTime notBefore = LocalDateTime.of(2026, 8, 30, 0, 0);
        LocalDateTime stalledBefore = LocalDateTime.now().minusMinutes(10);

        // notBefore 이전(레거시 backfill 잔재 시뮬레이션)
        Long legacyId = saveWithCreateDate(notBefore.minusDays(120));
        // notBefore 이후(정상적으로 발생한 진짜 stall)
        Long recentId = saveWithCreateDate(notBefore.plusMinutes(5));

        List<FcmMessage> candidates = fcmMessageRepository.findStalledCandidates(
                FcmSendStatus.PENDING, notBefore, stalledBefore, PageRequest.of(0, 20));

        List<Long> ids = candidates.stream().map(FcmMessage::getId).toList();
        assertThat(ids).contains(recentId);
        assertThat(ids).doesNotContain(legacyId);
    }

    @Test
    @DisplayName("findStalledCandidates는 최근에 수정된(아직 처리 중일 수 있는) 행을 제외한다")
    void findStalledCandidatesExcludesRecentlyModifiedRows() {
        LocalDateTime notBefore = LocalDateTime.now().minusDays(365);
        LocalDateTime stalledBefore = LocalDateTime.now().minusMinutes(10);

        Long freshId = saveWithTimestamps(notBefore.plusDays(1), LocalDateTime.now().minusSeconds(5));
        Long stalledId = saveWithTimestamps(notBefore.plusDays(1), LocalDateTime.now().minusMinutes(30));

        List<FcmMessage> candidates = fcmMessageRepository.findStalledCandidates(
                FcmSendStatus.PENDING, notBefore, stalledBefore, PageRequest.of(0, 20));

        List<Long> ids = candidates.stream().map(FcmMessage::getId).toList();
        assertThat(ids).contains(stalledId);
        assertThat(ids).doesNotContain(freshId);
    }

    @Test
    @DisplayName("leasePendingForRecovery는 PENDING 행에 대해 정확히 한 번만 1을 반환한다")
    void leasePendingForRecoveryIsAtomic() {
        Long id = saveWithCreateDate(LocalDateTime.now().minusDays(1));

        int firstAttempt = fcmMessageRepository.leasePendingForRecovery(id);
        int secondAttempt = fcmMessageRepository.leasePendingForRecovery(id);

        assertThat(firstAttempt).isEqualTo(1);
        assertThat(secondAttempt).isZero();

        entityManager.clear();
        FcmMessage reloaded = fcmMessageRepository.findById(id).orElseThrow();
        assertThat(reloaded.getSendStatus()).isEqualTo(FcmSendStatus.PROCESSING);
    }

    @Test
    @DisplayName("leasePendingForRecovery는 이미 PROCESSING인 행을 선점하지 못한다")
    void leasePendingForRecoveryFailsOnAlreadyProcessingRow() {
        Long id = saveWithStatus(FcmSendStatus.PROCESSING);

        int result = fcmMessageRepository.leasePendingForRecovery(id);

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("abandonPendingOlderThan은 notBefore~maxAgeBefore 구간의 PENDING만 ABANDONED로 종결한다")
    void abandonPendingOlderThanRespectsBothBounds() {
        LocalDateTime notBefore = LocalDateTime.of(2026, 8, 30, 0, 0);
        LocalDateTime maxAgeBefore = LocalDateTime.now().minusHours(1);

        Long tooOldId = saveWithCreateDate(notBefore.minusDays(10)); // notBefore 이전 → 대상 아님
        Long staleId = saveWithCreateDate(notBefore.plusHours(1));   // notBefore~maxAgeBefore 구간 → 대상
        Long freshId = saveWithCreateDate(LocalDateTime.now());       // maxAgeBefore 이후 → 대상 아님

        int abandoned = fcmMessageRepository.abandonPendingOlderThan(notBefore, maxAgeBefore);

        assertThat(abandoned).isEqualTo(1);
        entityManager.clear();
        assertThat(fcmMessageRepository.findById(tooOldId).orElseThrow().getSendStatus()).isEqualTo(FcmSendStatus.PENDING);
        assertThat(fcmMessageRepository.findById(staleId).orElseThrow().getSendStatus()).isEqualTo(FcmSendStatus.ABANDONED);
        assertThat(fcmMessageRepository.findById(freshId).orElseThrow().getSendStatus()).isEqualTo(FcmSendStatus.PENDING);
    }

    private Long saveWithCreateDate(LocalDateTime createDate) {
        return saveWithTimestamps(createDate, createDate);
    }

    private Long saveWithTimestamps(LocalDateTime createDate, LocalDateTime modifiedDate) {
        FcmMessage message = FcmMessage.builder().title("t").body("b").build();
        message.markPending(1);
        fcmMessageRepository.saveAndFlush(message);

        // create_date는 @Column(updatable = false)라 JPA 갱신 경로로는 못 바꾼다.
        // 경계 조건을 직접 통제하기 위해 네이티브 UPDATE로 우회한다.
        entityManager.createNativeQuery("UPDATE fcm_message SET create_date = ?1, modified_date = ?2 WHERE id = ?3")
                .setParameter(1, createDate)
                .setParameter(2, modifiedDate)
                .setParameter(3, message.getId())
                .executeUpdate();
        entityManager.clear();

        return message.getId();
    }

    private Long saveWithStatus(FcmSendStatus status) {
        FcmMessage message = FcmMessage.builder().title("t").body("b").sendStatus(status).build();
        fcmMessageRepository.saveAndFlush(message);
        entityManager.clear();
        return message.getId();
    }
}
