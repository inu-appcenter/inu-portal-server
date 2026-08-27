package kr.inuappcenterportal.inuportal.domain.firebase.repository;

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

    boolean existsByMemberIdAndIsReadFalse(Long memberId);

    boolean existsByMemberIdAndIsReadFalseAndViewCountLessThan(Long memberId, int viewCount);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberFcmMessage m SET m.isRead = true, m.readAt = :now WHERE m.memberId = :memberId AND m.isRead = false AND m.viewCount >= :threshold")
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

    @Modifying
    @Query("""
                UPDATE MemberFcmMessage m
                SET m.isRead = true,
                    m.readAt = :readAt
                WHERE m.memberId = :memberId
                  AND m.isRead = false
            """)
    int markAllAsReadByMemberId(Long memberId, LocalDateTime readAt);

    int countByMemberIdAndIsReadFalse(Long memberId);
}
