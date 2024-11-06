package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Fire;
import kr.inuappcenterportal.inuportal.domain.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FireRepository extends JpaRepository<Fire,Long> {
    Page<Fire> findByMemberOrderByIdDesc(Member member, Pageable pageable);
}
