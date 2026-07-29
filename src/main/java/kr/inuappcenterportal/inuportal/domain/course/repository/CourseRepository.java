package kr.inuappcenterportal.inuportal.domain.course.repository;

import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByTitleAndDepartment(String title, Department department);

    List<Course> findAllByDepartment(Department department);

    List<Course> findAllByActiveTrue();

    List<Course> findAllByDepartmentAndActiveTrue(Department department);

    Optional<Course> findByCourseCode(String CourseCode);
}
