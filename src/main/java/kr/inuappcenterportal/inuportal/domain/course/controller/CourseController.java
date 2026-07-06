package kr.inuappcenterportal.inuportal.domain.course.controller;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseCrawlerService;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseService;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController implements CourseApiSpecification {

    private final CourseService courseService;
    private final CourseCrawlerService courseCrawlerService;

    /**
     * 학과별 강의 조회 컨트롤러
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<CourseResponseDto>>> getCourses(
            @RequestParam(required = false) Department department
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.of(courseService.getCourses(department), "강의 목록 조회 성공"));
    }

    /**
     * 강의 동기화 컨트롤러 (관리자 전용)
     */
    @PostMapping("/sync")
    public ResponseEntity<ResponseDto<Void>> syncCourses() {
        courseCrawlerService.syncBaseCourses();

        return ResponseEntity.ok(
                ResponseDto.of(null, "강의 동기화 성공")
        );
    }
}
