package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
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

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberFcmMessage m SET m.isRead = true, m.readAt = :now WHERE m.memberId = :memberId AND m.isRead = false AND m.viewCount >= :threshold")
    int markAsReadByViewCount(@Param("memberId") Long memberId, @Param("threshold") int threshold, @Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE MemberFcmMessage m SET m.viewCount = m.viewCount + 1 WHERE m.memberId = :memberId AND m.isRead = false")
    int incrementViewCountForAllUnread(@Param("memberId") Long memberId);

    Optional<MemberFcmMessage> findByIdAndMemberId(Long id, Long memberId);
}
