package kr.inuappcenterportal.inuportal.domain.course.repository;

import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseMeetingRepository extends JpaRepository<CourseMeeting, Long> {
}
