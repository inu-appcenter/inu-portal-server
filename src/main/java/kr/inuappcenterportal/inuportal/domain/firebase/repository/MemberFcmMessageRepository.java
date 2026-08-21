package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface MemberFcmMessageRepository extends JpaRepository<MemberFcmMessage, Long> {

    Page<MemberFcmMessage> findAllByMemberId(Long memberId, Pageable pageable);

    boolean existsByMemberIdAndIsReadFalse(Long memberId);

    Optional<MemberFcmMessage> findByIdAndMemberId(Long id, Long memberId);

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
