package kr.inuappcenterportal.inuportal.domain.notice.repository;

import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.enums.NoticeContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findAllByCategory(String category, Pageable pageable);

    Page<Notice> findAllBy(Pageable pageable);

    Optional<Notice> findByUrl(String url);

    List<Notice> findAllByCategoryAndCreateDateGreaterThanEqual(String category, String createDate);

    List<Notice> findAllByCategoryAndCreateDateGreaterThanAndCreateDateLessThan(String category, String oldestDate, String newestDate);

    @Query("SELECT n FROM Notice n WHERE (:category IS NULL OR n.category = :category) AND (n.title LIKE %:query% OR n.writer LIKE %:query%)")
    Page<Notice> searchNotices(@Param("query") String query, @Param("category") String category, Pageable pageable);

    @Query("SELECT n FROM Notice n  ORDER BY n.id DESC LIMIT 12")
    List<Notice> findTop12();

    // 제목으로
    Optional<Notice> findFirstByTitleContainingOrderByCreateDateDesc(String keyword);

    @Query("""
            select n from Notice n
            left join fetch n.content
            where (
                    (
                        (n.content is null
                         or n.content.contentText is null
                         or n.content.inlineImageUrlsJson is null
                         or n.content.attachmentMetaJson is null)
                        and (n.contentStatus is null or n.contentStatus in :statuses)
                    )
                    or (
                        n.createDate >= :refreshThresholdDate
                        and (n.contentFetchedAt is null or n.contentFetchedAt <= :fetchedBefore)
                        and (n.contentStatus is null or n.contentStatus not in :excludedStatuses)
                    )
            )
            order by n.id desc
            """)
    List<Notice> findBackfillTargets(
            @Param("statuses") List<NoticeContentStatus> statuses,
            @Param("refreshThresholdDate") String refreshThresholdDate,
            @Param("fetchedBefore") LocalDateTime fetchedBefore,
            @Param("excludedStatuses") List<NoticeContentStatus> excludedStatuses,
            Pageable pageable
    );

    @Query(value = "select n from Notice n left join fetch n.content where (:category is null or n.category = :category)",
           countQuery = "select count(n) from Notice n where (:category is null or n.category = :category)")
    Page<Notice> findAllWithContentByCategory(@Param("category") String category, Pageable pageable);
}

