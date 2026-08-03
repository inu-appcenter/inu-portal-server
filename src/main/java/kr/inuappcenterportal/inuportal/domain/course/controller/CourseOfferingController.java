package kr.inuappcenterportal.inuportal.domain.course.controller;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseOfferingService;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseOfferingSyncService;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course-offerings")
public class CourseOfferingController implements CourseOfferingApiSpecification {

    private final CourseOfferingSyncService courseOfferingSyncService;
    private final CourseOfferingService courseOfferingService;

    @PostMapping("/sync")
    public ResponseEntity<ResponseDto<Void>> syncCourseOffering(
            @RequestParam int year,
            @RequestParam String modDate
    ) {
        courseOfferingSyncService.syncCourseWithSchoolApi(year, modDate);
        return ResponseEntity.ok(ResponseDto.of(null, "개설 강의 동기화 성공"));
    }

    @GetMapping
    public ResponseEntity<ResponseDto<Page<CourseOfferingResponseDto>>> getCourseOfferings(
            @RequestParam Integer year,
            @RequestParam SemesterTerm term,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String collegeName,
            @RequestParam(required = false) String hyName,
            @RequestParam(required = false) String isuName,
            @RequestParam(required = false) String isuFldName,
            @RequestParam(required = false) String ssupTypeName,
            @RequestParam(required = false) String englishName,
            @RequestParam(required = false) Integer credit,
            @RequestParam(required = false) String keyword,
            @ParameterObject @PageableDefault(size = 50) Pageable pageable
    ) {
        return ResponseEntity.ok(
                ResponseDto.of(
                        courseOfferingService.getCourseOfferings(
                                year,
                                term,
                                deptName,
                                collegeName,
                                hyName,
                                isuName,
                                isuFldName,
                                ssupTypeName,
                                englishName,
                                credit,
                                keyword,
                                pageable
                        ),
                        "개설 강의 목록 조회 성공"
                )
        );
    }
}
