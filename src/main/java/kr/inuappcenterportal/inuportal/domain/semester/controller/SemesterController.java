package kr.inuappcenterportal.inuportal.domain.semester.controller;

import kr.inuappcenterportal.inuportal.domain.semester.dto.SemesterResponseDto;
import kr.inuappcenterportal.inuportal.domain.semester.service.SemesterService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/semesters")
public class SemesterController implements SemesterApiSpecification {

    private final SemesterService semesterService;

    /**
     * 유효한 학기 조회 메서드(Open, Closed)
     */
    @GetMapping
    public ResponseEntity<List<SemesterResponseDto>> getValidSemesters() {
        List<SemesterResponseDto> semesters = semesterService.getValidSemesters();
        return ResponseEntity.ok(semesters);
    }

    /**
     * 학기 동기화 컨트롤러 (관리자 전용)
     */
    @PostMapping("/sync")
    public ResponseEntity<ResponseDto<Void>> syncSemester() {
        semesterService.syncSemestersByYear();

        return ResponseEntity.ok(
                ResponseDto.of(null, "학기 동기화 성공")
        );
    }
}
