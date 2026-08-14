package kr.inuappcenterportal.inuportal.domain.mockRegistration.repository;

import kr.inuappcenterportal.inuportal.domain.mockRegistration.model.MockWatchlistItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MockWatchlistRepository extends JpaRepository<MockWatchlistItem, Long> {
    @EntityGraph(attributePaths = {"courseOffering", "courseOffering.course", "courseOffering.semester"})
    List<MockWatchlistItem> findAllByMemberIdAndSemesterId(Long memberId, Long semesterId);
    Optional<MockWatchlistItem> findByMemberIdAndSemesterIdAndCourseOfferingId(Long memberId, Long semesterId, Long offeringId);
    boolean existsByMemberIdAndSemesterIdAndCourseOfferingId(Long memberId, Long semesterId, Long offeringId);
    long countByMemberIdAndSemesterId(Long memberId, Long semesterId);
    void deleteAllBySemesterId(Long semesterId);
}
