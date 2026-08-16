package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
}
