package kr.inuappcenterportal.inuportal.domain.firebase.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FcmMessageRepository extends JpaRepository<FcmMessage, Long> {

    Page<FcmMessage> findAllByAdminMessageTrue(Pageable pageable);

    Optional<FcmMessage> findByIdAndAdminMessageTrue(Long id);
}
