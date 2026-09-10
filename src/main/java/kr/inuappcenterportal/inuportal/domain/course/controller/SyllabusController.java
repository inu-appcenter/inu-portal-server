package kr.inuappcenterportal.inuportal.domain.course.controller;

import kr.inuappcenterportal.inuportal.domain.course.service.SyllabusService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/syllabus")
@RequiredArgsConstructor
public class SyllabusController {

    private final SyllabusService syllabusService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<Void>> importSyllabus(
            @RequestPart("file") MultipartFile file
    ) {
        syllabusService.importFromJson(file);
        return ResponseEntity.ok(
                ResponseDto.of(null, "강의계획서 적제 성공")
        );
    }
}
