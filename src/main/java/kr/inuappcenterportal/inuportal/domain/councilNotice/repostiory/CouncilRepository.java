package kr.inuappcenterportal.inuportal.domain.councilNotice.repostiory;

import kr.inuappcenterportal.inuportal.domain.councilNotice.model.CouncilNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouncilRepository extends JpaRepository<CouncilNotice, Long> {
    Page<CouncilNotice> findAllBy(Pageable pageable);
}
