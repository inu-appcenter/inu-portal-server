package kr.inuappcenterportal.inuportal.domain.keyword.repository;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.keyword.domain.Keyword;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    @Query("SELECT k FROM Keyword k " +
            "WHERE :title LIKE CONCAT('%', k.keyword, '%') " +
            "AND k.department = :department")
    List<Keyword> findKeywordsByKeywordAndDepartmentMatches(@Param("title") String title,
                                                          @Param("department")Department department);

    List<Keyword> findAllByMemberId(Long memberId);

    boolean existsByIdAndMemberId(Long id, Long memberId);

    List<Keyword> findAllByMemberIdAndKeywordIsNull(Long memberId);

    @Query("SELECT k FROM Keyword k WHERE k.department = :department AND k.keyword IS NULL")
    List<Keyword> findKeywordsByDepartmentAndKeywordIsNull(Department department);

    @Query("SELECT k FROM Keyword k " +
            "WHERE :title LIKE CONCAT('%', k.keyword, '%') " +
            "AND (k.category IS NULL OR k.category = :category) " +
            "AND k.type = 'SCHOOL_NOTICE'")
    List<Keyword> findKeywordsByKeywordAndCategoryMatches(@Param("title") String title,
                                                        @Param("category") String category);

    @Query("SELECT k FROM Keyword k WHERE k.category = :category AND k.keyword IS NULL AND k.type = 'SCHOOL_NOTICE'")
    List<Keyword> findKeywordsByCategoryAndKeywordIsNull(@Param("category") String category);

    List<Keyword> findAllByMemberIdAndKeywordIsNullAndType(Long memberId, kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type);

        @Modifying(clearAutomatically = true)
        @Query("DELETE FROM Keyword k WHERE k.memberId = :memberId AND k.type = :type")
        void deleteAllByMemberIdAndType(@Param("memberId") Long memberId,
                                        @Param("type") kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type);


    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Keyword k WHERE k.memberId = :memberId AND k.type = :type AND k.keyword IS NULL")
    void deleteByMemberIdAndTypeAndKeywordIsNull(@Param("memberId") Long memberId,
                                               @Param("type") kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType type);

    // SCHOOL_NOTICE 타입 중 특정 카테고리와 일치하며 키워드가 null인 데이터 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Keyword k " +
            "WHERE k.memberId = :memberId " +
            "AND k.type = 'SCHOOL_NOTICE' " +
            "AND k.category = :category " +
            "AND k.keyword IS NULL")
    void deleteSchoolNoticeByCategoryAndKeywordIsNull(@Param("memberId") Long memberId,
                                                      @Param("category") String category);

    // SCHOOL_NOTICE 타입 중 키워드가 null인 모든 데이터 삭제
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Keyword k " +
            "WHERE k.memberId = :memberId " +
            "AND k.type = 'SCHOOL_NOTICE' " +
            "AND k.keyword IS NULL")
    void deleteSchoolNoticeByKeywordIsNull(@Param("memberId") Long memberId);
}