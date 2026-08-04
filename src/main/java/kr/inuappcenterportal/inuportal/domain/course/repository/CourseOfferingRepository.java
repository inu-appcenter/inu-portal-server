package kr.inuappcenterportal.inuportal.domain.course.repository;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long>, CourseOfferingRepositoryCustom {

    Optional<CourseOffering> findBySemesterIdAndSubjectNumber(Long semesterId, String subjectNumber);

    @EntityGraph(attributePaths = {"course", "semester"})
    Page<CourseOffering> findAllBySemesterId(Long semesterId, Pageable pageable);
}
