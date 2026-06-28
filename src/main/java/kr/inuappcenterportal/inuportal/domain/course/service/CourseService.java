package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * 강의 조회 메서드
     */
    @Transactional(readOnly = true)
    public void getCourse() {

    }

    /**
     * 강의 생성 메서드
     */
    @Transactional
    public void createCourse() {

    }

    /**
     * 강의 수정 메서드
     */
    @Transactional
    public void updateCourse() {

    }

    /**
     * 강의 삭제 메서드
     */
    @Transactional
    public void deleteCourse() {

    }
}
