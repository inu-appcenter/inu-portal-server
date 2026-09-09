package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.NotificationReadSource;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberFcmMessageRepository extends JpaRepository<MemberFcmMessage, Long> {

    Page<MemberFcmMessage> findAllByMemberId(Long memberId, Pageable pageable);

    /** 유실 보정 시 원래 수신 대상을 복원하기 위해 사용한다. */
    @Query("SELECT m.memberId FROM MemberFcmMessage m WHERE m.fcmMessageId = :fcmMessageId")
    List<Long> findMemberIdsByFcmMessageId(@Param("fcmMessageId") Long fcmMessageId);

    /**
     * 유실 보정 시 type을 복원하기 위해 사용한다. 한 fcmMessageId에는 항상 단일 type만
     * 연결돼야 하므로(같은 dispatch 안의 모든 수신자는 같은 type을 공유), 결과가 둘
     * 이상이면 호출부가 복원을 포기하도록 그대로 노출한다. 임의로 하나를 골라 잘못된
     * 라우팅을 재발송하는 사고(#431)를 피하기 위함이다.
     */
    @Query("SELECT DISTINCT m.fcmMessageType FROM MemberFcmMessage m WHERE m.fcmMessageId = :fcmMessageId")
    List<FcmMessageType> findDistinctTypesByFcmMessageId(@Param("fcmMessageId") Long fcmMessageId);

    boolean existsByMemberIdAndIsReadFalse(Long memberId);

    boolean existsByMemberIdAndIsReadFalseAndViewCountLessThan(Long memberId, int viewCount);

    /** 조회수 기반 자동 읽음. 사용자가 알림을 연 게 아니므로 read_source는 BULK로 남긴다. */
    @Modifying(clearAutomatically = true)
    @Query("""
                UPDATE MemberFcmMessage m
                SET m.isRead = true,
                    m.readAt = :now,
                    m.readSource = kr.inuappcenterportal.inuportal.domain.firebase.enums.NotificationReadSource.BULK
                WHERE m.memberId = :memberId
                  AND m.isRead = false
                  AND m.viewCount >= :threshold
            """)
    int markAsReadByViewCount(@Param("memberId") Long memberId, @Param("threshold") int threshold, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberFcmMessage m SET m.viewCount = m.viewCount + 1 WHERE m.memberId = :memberId AND m.isRead = false")
    int incrementViewCountForAllUnread(@Param("memberId") Long memberId);

    Optional<MemberFcmMessage> findByIdAndMemberId(Long id, Long memberId);

    /**
     * 푸시 payload의 공통 식별자(fcmMessageId)와 인증된 회원으로 개인 알림함 행을 찾는다.
     * (fcm_message_id, member_id)는 유일해야 하지만 DB 제약이 아직 없으므로 목록으로 받는다.
     */
    List<MemberFcmMessage> findAllByFcmMessageIdAndMemberId(Long fcmMessageId, Long memberId);

    @Query("SELECT m.fcmMessageType FROM MemberFcmMessage m WHERE m.fcmMessageId = :fcmMessageId")
    List<FcmMessageType> findTypesByFcmMessageId(@Param("fcmMessageId") Long fcmMessageId);

    /** 전체 읽음. 개별 클릭이 아니므로 read_source는 BULK로 남긴다. */
    @Modifying
    @Query("""
                UPDATE MemberFcmMessage m
                SET m.isRead = true,
                    m.readAt = :readAt,
                    m.readSource = kr.inuappcenterportal.inuportal.domain.firebase.enums.NotificationReadSource.BULK
                WHERE m.memberId = :memberId
                  AND m.isRead = false
            """)
    int markAllAsReadByMemberId(Long memberId, LocalDateTime readAt);

    int countByMemberIdAndIsReadFalse(Long memberId);

    /**
     * 발송 로그(fcmMessageId)별 수신/읽음 집계. 관리자 페이지 클릭율(#466)의 원천이다.
     *
     * <p>목록 API가 페이지당 8건을 한 번에 그리므로 건별 조회 대신 IN + GROUP BY 한 방으로 센다.
     * 반환 순서는 [fcmMessageId, 수신 회원 수, 읽음 수, PUSH 클릭 수, INBOX 클릭 수]다.
     * read_source가 도입되기 전에 읽힌 행은 source가 null이라 읽음 수에만 잡히고 클릭 수에는
     * 빠진다(과거 발송의 클릭율이 0으로 보이는 건 데이터가 없어서지 버그가 아니다).
     */
    @Query("""
                SELECT m.fcmMessageId,
                       COUNT(m),
                       SUM(CASE WHEN m.isRead = true THEN 1 ELSE 0 END),
                       SUM(CASE WHEN m.readSource = :push THEN 1 ELSE 0 END),
                       SUM(CASE WHEN m.readSource = :inbox THEN 1 ELSE 0 END)
                FROM MemberFcmMessage m
                WHERE m.fcmMessageId IN :fcmMessageIds
                GROUP BY m.fcmMessageId
            """)
    List<Object[]> aggregateReadStatsByFcmMessageIds(@Param("fcmMessageIds") List<Long> fcmMessageIds,
                                                     @Param("push") NotificationReadSource push,
                                                     @Param("inbox") NotificationReadSource inbox);
}
