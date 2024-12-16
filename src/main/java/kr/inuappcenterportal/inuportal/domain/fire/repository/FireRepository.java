package kr.inuappcenterportal.inuportal.domain.fire.repository;

import kr.inuappcenterportal.inuportal.domain.fire.model.Fire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FireRepository extends JpaRepository<Fire,Long> {
    Page<Fire> findByMemberIdOrderByIdDesc(Long member_id, Pageable pageable);
    Optional<Fire> findByRequestId(String requestId);
}
