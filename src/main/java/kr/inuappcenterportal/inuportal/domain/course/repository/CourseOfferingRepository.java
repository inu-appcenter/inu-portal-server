package kr.inuappcenterportal.inuportal.domain.course.repository;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long>, CourseOfferingRepositoryCustom {

    Optional<CourseOffering> findBySemesterIdAndSubjectNumber(Long semesterId, String subjectNumber);

    @EntityGraph(attributePaths = {"course", "semester"})
    List<CourseOffering> findAllBySemesterId(Long semesterId);

    @EntityGraph(attributePaths = {"course", "semester"})
    List<CourseOffering> findAllByCourseCourseCodeIn(List<String> courseCodes);
}
