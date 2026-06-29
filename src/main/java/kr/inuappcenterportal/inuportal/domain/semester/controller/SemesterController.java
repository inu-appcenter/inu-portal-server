package kr.inuappcenterportal.inuportal.domain.semester.controller;

import kr.inuappcenterportal.inuportal.domain.semester.dto.SemesterResponseDto;
import kr.inuappcenterportal.inuportal.domain.semester.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/semesters")
public class SemesterController {

    private final SemesterService semesterService;

    /**
     * 유효한 학기 조회 메서드(Open, Closed)
     */
    @GetMapping
    public ResponseEntity<List<SemesterResponseDto>> getValidSemesters(
    ) {

        List<SemesterResponseDto> semesters = semesterService.getValidSemesters();
        return ResponseEntity.ok(semesters);
    }
}
