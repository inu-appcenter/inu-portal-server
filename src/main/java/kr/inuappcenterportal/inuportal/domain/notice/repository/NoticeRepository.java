package kr.inuappcenterportal.inuportal.domain.notice.repository;

import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice,Long> {

    Page<Notice> findAllByCategory(String category, Pageable pageable);
    Page<Notice> findAllBy(Pageable pageable);
    @Query("SELECT n FROM Notice n  ORDER BY n.view DESC LIMIT 12")
    List<Notice> findTop12();
}
