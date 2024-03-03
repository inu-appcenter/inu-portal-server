package kr.inuappcenterportal.inuportal.repository;

import kr.inuappcenterportal.inuportal.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice,Long> {

    @Modifying
    @Transactional
    @Query(value = "truncate table notice",nativeQuery = true)
    void truncateTable();

    List<Notice> findAllByCategory(String category);
    List<Notice> findAllByCategoryOrderByViewDesc(String category);
    List<Notice> findAllByOrderByDateDesc();
    List<Notice> findAllByOrderByViewDesc();
}
