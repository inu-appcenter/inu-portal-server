package kr.inuappcenterportal.inuportal.domain.mockRegistration.repository;

import kr.inuappcenterportal.inuportal.domain.mockRegistration.model.MockEnrollment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MockEnrollmentRepository extends JpaRepository<MockEnrollment, Long> {
    @EntityGraph(attributePaths = {"courseOffering", "courseOffering.course", "courseOffering.semester"})
    List<MockEnrollment> findAllByMemberIdAndSemesterId(Long memberId, Long semesterId);
    Optional<MockEnrollment> findByMemberIdAndSemesterIdAndCourseOfferingId(Long memberId, Long semesterId, Long offeringId);
    boolean existsByMemberIdAndSemesterIdAndCourseOfferingId(Long memberId, Long semesterId, Long offeringId);
    void deleteAllBySemesterId(Long semesterId);
}
