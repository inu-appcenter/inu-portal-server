package kr.inuappcenterportal.inuportal.domain.notice.repository;

import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice,Long> {

    Page<Notice> findAllByCategory(String category, Pageable pageable);
    Page<Notice> findAllBy(Pageable pageable);
    Optional<Notice> findByUrl(String url);
    List<Notice> findAllByCategoryAndCreateDateGreaterThanEqual(String category, String createDate);
    List<Notice> findAllByCategoryAndCreateDateGreaterThanAndCreateDateLessThan(String category, String oldestDate, String newestDate);
    @Query("SELECT n FROM Notice n WHERE (:category IS NULL OR n.category = :category) AND (n.title LIKE %:query% OR n.writer LIKE %:query%)")
    Page<Notice> searchNotices(@Param("query") String query, @Param("category") String category, Pageable pageable);
    @Query("SELECT n FROM Notice n  ORDER BY n.id DESC LIMIT 12")
    List<Notice> findTop12();
}
