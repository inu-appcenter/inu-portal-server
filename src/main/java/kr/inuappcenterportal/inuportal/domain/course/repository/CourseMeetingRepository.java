package kr.inuappcenterportal.inuportal.domain.course.repository;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseMeetingRepository extends JpaRepository<CourseMeeting, Long> {
    List<CourseMeeting> findAllByCourseOfferingId(Long courseOfferingId);
}
