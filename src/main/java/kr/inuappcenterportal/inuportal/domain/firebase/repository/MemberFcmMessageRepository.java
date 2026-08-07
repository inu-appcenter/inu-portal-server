package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberFcmMessageRepository extends JpaRepository<MemberFcmMessage, Long> {

    Page<MemberFcmMessage> findAllByMemberId(Long memberId, Pageable pageable);

    boolean existsByMemberIdAndIsReadFalse(Long memberId);

    Optional<MemberFcmMessage> findByIdAndMemberId(Long id, Long memberId);
}
