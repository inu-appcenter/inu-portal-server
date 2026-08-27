package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmSendStatus;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FcmMessageRepository extends JpaRepository<FcmMessage, Long> {

    Page<FcmMessage> findAllByAdminMessageTrue(Pageable pageable);

    Optional<FcmMessage> findByIdAndAdminMessageTrue(Long id);

    /**
     * 발송이 시작되지 못한 채 오래 머물러 있는 알림을 찾는다.
     * AFTER_COMMIT 이벤트가 유실된 경우(커밋 직후 애플리케이션 종료 등) 여기에 남는다.
     */
    List<FcmMessage> findAllBySendStatusAndModifiedDateBefore(FcmSendStatus sendStatus, LocalDateTime threshold);
}
