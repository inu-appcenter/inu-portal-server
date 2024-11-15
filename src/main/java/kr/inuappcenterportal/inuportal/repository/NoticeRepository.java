package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice,Long> {

    @Modifying
    @Transactional
    @Query(value = "truncate table notice", nativeQuery = true)
    void truncateTable();

    List<Notice> findAllByCategory(String category, Pageable pageable);

    List<Notice> findAllByCategoryOrderByViewDesc(String category, Pageable pageable);

    List<Notice> findAllByOrderByCreateDateDesc(Pageable pageable);

    List<Notice> findAllByOrderByViewDesc(Pageable pageable);

    Long countAllByCategory(String category);

    long count();
    @Query("SELECT n FROM Notice n  ORDER BY n.view DESC LIMIT 12")
    List<Notice> findTop12();
}
