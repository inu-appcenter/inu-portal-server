package kr.inuappcenterportal.inuportal.domain.course.controller;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseMeeting.MeetingFilterMode;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseOfferingService;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseOfferingSyncService;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course-offerings")
public class CourseOfferingController implements CourseOfferingApiSpecification {

    private static final int COURSE_OFFERING_PAGE_SIZE = 50;

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
            @RequestParam(required = false) List<String> hyNames,
            @RequestParam(required = false) List<String> isuNames,
            @RequestParam(required = false) List<String> isuFldNames,
            @RequestParam(required = false) List<String> ssupTypeNames,
            @RequestParam(required = false) List<Integer> credits,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MeetingFilterMode meetingFilterMode,
            @RequestParam(required = false) List<String> meetings,
            @RequestParam(defaultValue = "0") Integer page
    ) {
        Pageable pageable = PageRequest.of(page, COURSE_OFFERING_PAGE_SIZE);

        return ResponseEntity.ok(
                ResponseDto.of(
                        courseOfferingService.getCourseOfferings(
                                year,
                                term,
                                deptName,
                                collegeName,
                                hyNames,
                                isuNames,
                                isuFldNames,
                                ssupTypeNames,
                                credits,
                                keyword,
                                meetingFilterMode,
                                meetings,
                                pageable
                        ),
                        "개설 강의 목록 조회 성공"
                )
        );
    }
}
