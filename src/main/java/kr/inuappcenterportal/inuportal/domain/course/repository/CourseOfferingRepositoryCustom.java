package kr.inuappcenterportal.inuportal.domain.course.repository;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingSearchCondition;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CourseOfferingRepositoryCustom {

    Page<CourseOffering> search(CourseOfferingSearchCondition searchCondition, Pageable pageable);
}
