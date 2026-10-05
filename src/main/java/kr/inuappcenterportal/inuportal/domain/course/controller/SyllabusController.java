package kr.inuappcenterportal.inuportal.domain.course.controller;

import kr.inuappcenterportal.inuportal.domain.course.dto.syllabus.SyllabusResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.service.SyllabusService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/syllabus")
@RequiredArgsConstructor
public class SyllabusController implements SyllabusApiSpecification {

    private final SyllabusService syllabusService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Void>> importSyllabus(
            @RequestPart("file") MultipartFile file
    ) {
        syllabusService.importFromJson(file);
        return ResponseEntity.ok(
                ResponseDto.of(null, "강의계획서 적재 성공")
        );
    }

    @kr.inuappcenterportal.inuportal.domain.agent.openapi.annotation.AgentExposed(
            name = "syllabus",
            description = "교과목의 강의계획서(수업 개요, 주차별 수업 계획, 평가 비율, 과제 및 교재 정보)를 상세 조회합니다.",
            capabilities = {"강의계획서 상세 조회", "평가 기준 및 시험 비율 확인", "주차별 강의 일정 확인"},
            triggerExamples = {"데이터사이언스 강의계획서 보여줘", "이 수업 과제 몇 번 있어?"},
            cardTitle = "강의계획서 상세",
            requiresLogin = false
    )
    @GetMapping
    public ResponseEntity<ResponseDto<SyllabusResponseDto>> getSyllabus(
            @io.swagger.v3.oas.annotations.Parameter(description = "개설 강의 ID (CourseOffering ID). API_COURSE_OFFERINGS 결과의 id 필드값", required = true)
            @RequestParam Long courseOfferingId
    ) {
        SyllabusResponseDto response = syllabusService.getSyllabus(courseOfferingId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "강의계획서 조회 성공")
        );
    }
}
