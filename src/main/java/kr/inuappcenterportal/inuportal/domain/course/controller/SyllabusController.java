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

    @GetMapping
    public ResponseEntity<ResponseDto<SyllabusResponseDto>> getSyllabus(
            @RequestParam Long courseOfferingId
    ) {
        SyllabusResponseDto response = syllabusService.getSyllabus(courseOfferingId);

        return ResponseEntity.ok(
                ResponseDto.of(response, "강의계획서 조회 성공")
        );
    }
}
