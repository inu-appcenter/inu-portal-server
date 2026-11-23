package kr.inuappcenterportal.inuportal.domain.mockRegistration.repository;

import kr.inuappcenterportal.inuportal.domain.mockRegistration.model.MockWatchlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MockWatchlistRepository extends JpaRepository<MockWatchlistItem, Long> {
    @EntityGraph(attributePaths = {"courseOffering", "courseOffering.course", "courseOffering.semester"})
    List<MockWatchlistItem> findAllByMemberIdAndSemesterId(Long memberId, Long semesterId);
    Optional<MockWatchlistItem> findByMemberIdAndSemesterIdAndCourseOfferingId(Long memberId, Long semesterId, Long offeringId);
    boolean existsByMemberIdAndSemesterIdAndCourseOfferingId(Long memberId, Long semesterId, Long offeringId);
    long countByMemberIdAndSemesterId(Long memberId, Long semesterId);
    void deleteAllBySemesterId(Long semesterId);

    // 회원탈퇴 시 해당 회원의 데이터를 한 번에 지운다.
    @Modifying(flushAutomatically = true)
    @Query("delete from MockWatchlistItem w where w.member.id = :memberId")
    int deleteAllByMemberIdForWithdrawal(@Param("memberId") Long memberId);
}
