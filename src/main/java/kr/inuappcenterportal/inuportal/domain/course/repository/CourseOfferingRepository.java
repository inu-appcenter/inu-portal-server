package kr.inuappcenterportal.inuportal.domain.course.repository;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {
}
