package kr.inuappcenterportal.inuportal.firebase;

import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.NotificationReadSource;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 페이지 클릭율(#466)의 원천 집계를 검증한다.
 * 핵심은 "전체 읽음(BULK)을 클릭으로 세지 않는다"는 것이다. 여기가 무너지면 전환율이 부풀려진다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class MemberFcmMessageReadStatsTest {

    @Autowired
    MemberFcmMessageRepository memberFcmMessageRepository;

    @Test
    @DisplayName("발송별 집계는 수신 수와 읽음 수를 세고, 클릭은 PUSH/INBOX만 센다")
    void aggregateReadStatsSeparatesClicksFromBulkReads() {
        long messageId = 9001L;
        save(messageId, 1L, NotificationReadSource.PUSH);
        save(messageId, 2L, NotificationReadSource.INBOX);
        save(messageId, 3L, NotificationReadSource.BULK);
        save(messageId, 4L, null); // 안 읽음
        // 다른 발송 건은 섞이지 않아야 한다.
        save(9002L, 5L, NotificationReadSource.PUSH);

        List<Object[]> rows = memberFcmMessageRepository.aggregateReadStatsByFcmMessageIds(
                List.of(messageId), NotificationReadSource.PUSH, NotificationReadSource.INBOX);

        assertThat(rows).hasSize(1);
        Object[] row = rows.get(0);
        assertThat((Long) row[0]).isEqualTo(messageId);
        assertThat(((Number) row[1]).intValue()).isEqualTo(4); // 수신
        assertThat(((Number) row[2]).intValue()).isEqualTo(3); // 읽음(BULK 포함)
        assertThat(((Number) row[3]).intValue()).isEqualTo(1); // PUSH 클릭
        assertThat(((Number) row[4]).intValue()).isEqualTo(1); // INBOX 클릭
    }

    @Test
    @DisplayName("전체 읽음 처리는 read_source를 BULK로 남겨 클릭으로 잡히지 않는다")
    void markAllAsReadIsNotCountedAsClick() {
        long messageId = 9101L;
        long memberId = 11L;
        save(messageId, memberId, null);

        memberFcmMessageRepository.markAllAsReadByMemberId(memberId, LocalDateTime.now());

        Object[] row = memberFcmMessageRepository.aggregateReadStatsByFcmMessageIds(
                List.of(messageId), NotificationReadSource.PUSH, NotificationReadSource.INBOX).get(0);
        assertThat(((Number) row[2]).intValue()).isEqualTo(1); // 읽음으로는 잡히고
        assertThat(((Number) row[3]).intValue()).isZero();     // 클릭으로는 안 잡힌다
        assertThat(((Number) row[4]).intValue()).isZero();
    }

    @Test
    @DisplayName("자동 읽음(BULK) 뒤에 실제로 눌러서 열면 클릭으로 승격된다")
    void lateClickAfterBulkAutoReadIsPromotedToClick() {
        long messageId = 9201L;
        long memberId = 21L;
        MemberFcmMessage message = MemberFcmMessage.of(messageId, memberId, FcmMessageType.SCHOOL_NOTICE);
        // 알림함을 두 번 열어 조회수 기반 자동 읽음이 먼저 걸린 상태
        message.markAsRead(NotificationReadSource.BULK);
        memberFcmMessageRepository.saveAndFlush(message);

        // 그 뒤 사용자가 알림함에서 실제로 눌러서 열었다
        message.markAsRead(NotificationReadSource.INBOX);
        memberFcmMessageRepository.saveAndFlush(message);

        Object[] row = memberFcmMessageRepository.aggregateReadStatsByFcmMessageIds(
                List.of(messageId), NotificationReadSource.PUSH, NotificationReadSource.INBOX).get(0);
        assertThat(((Number) row[2]).intValue()).isEqualTo(1); // 읽음
        assertThat(((Number) row[4]).intValue())
                .as("자동 읽음이 먼저 걸렸다는 이유로 진짜 클릭이 사라지면 전환율이 과소 집계된다")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("이미 클릭으로 기록된 행은 뒤이은 일괄 읽음에 덮이지 않는다")
    void bulkReadDoesNotDowngradeAnExistingClick() {
        long messageId = 9301L;
        MemberFcmMessage message = MemberFcmMessage.of(messageId, 31L, FcmMessageType.SCHOOL_NOTICE);
        message.markAsRead(NotificationReadSource.PUSH);
        message.markAsRead(NotificationReadSource.BULK);
        memberFcmMessageRepository.saveAndFlush(message);

        Object[] row = memberFcmMessageRepository.aggregateReadStatsByFcmMessageIds(
                List.of(messageId), NotificationReadSource.PUSH, NotificationReadSource.INBOX).get(0);
        assertThat(((Number) row[3]).intValue()).isEqualTo(1);
    }

    private void save(long fcmMessageId, long memberId, NotificationReadSource readSource) {
        MemberFcmMessage message = MemberFcmMessage.of(fcmMessageId, memberId, FcmMessageType.SCHOOL_NOTICE);
        if (readSource != null) {
            message.markAsRead(readSource);
        }
        memberFcmMessageRepository.saveAndFlush(message);
    }
}
