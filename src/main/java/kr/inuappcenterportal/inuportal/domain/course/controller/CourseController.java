package kr.inuappcenterportal.inuportal.domain.course.controller;

import kr.inuappcenterportal.inuportal.domain.course.dto.CourseResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseService;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/courses")
public class CourseController implements CourseApiSpecification {
    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<ResponseDto<List<CourseResponseDto>>> getCourses(
            @RequestParam(required = false) Department department
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseDto.of(courseService.getCourses(department), "강의 목록 조회 성공"));
    }
}
