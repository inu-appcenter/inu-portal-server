package kr.inuappcenterportal.inuportal.domain.course.controller;

import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingOptionsResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.CourseOfferingSort;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.MeetingFilterMode;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseOfferingService;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseOfferingSyncService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal Member member,
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
            @RequestParam(required = false) CourseOfferingSort sort,
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
                                sort,
                                pageable,
                                canViewProfessor(member)
                        ),
                        "개설 강의 목록 조회 성공"
                )
        );
    }

    @GetMapping("/open")
    public ResponseEntity<ResponseDto<List<CourseOfferingResponseDto>>> getOpenCourseOfferings(
            @AuthenticationPrincipal Member member,
            @RequestParam(required = false) String deptCode,
            @RequestParam(required = false) String isuCode,
            @RequestParam(required = false) String isuFldCode,
            @RequestParam(required = false) String cnctrIsuCode,
            @RequestParam(required = false) Boolean hussOnly,
            @RequestParam(required = false) Boolean majorOnly,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(ResponseDto.of(courseOfferingService.getOpenCourseOfferings(
                deptCode, isuCode, isuFldCode, cnctrIsuCode, hussOnly, majorOnly, keyword, false), "현재 학기 개설 강의 조회 성공"));
    }

    @GetMapping("/open/options")
    public ResponseEntity<ResponseDto<CourseOfferingOptionsResponseDto>> getOpenCourseOfferingOptions() {
        return ResponseEntity.ok(ResponseDto.of(courseOfferingService.getOpenCourseOfferingOptions(), "현재 학기 강의 검색 옵션 조회 성공"));
    }

    // meeting·교수명은 이 API 용도(학점 계산에 필요한 학점/이수구분 등 채우기)에
    // 필요 없어 항상 뺀다.
    @GetMapping("/by-codes")
    public ResponseEntity<ResponseDto<List<CourseOfferingResponseDto>>> getCourseOfferingsByCodes(
            @RequestParam List<String> courseCodes
    ) {
        return ResponseEntity.ok(
                ResponseDto.of(
                        courseOfferingService.getCourseOfferingsByCourseCodes(courseCodes, false),
                        "개설 강의 목록 조회 성공"
                )
        );
    }

    private boolean canViewProfessor(Member member) {
        return member != null
                && member.getStudentId() != null
                && member.getStudentId().matches("\\d{9}");
    }
}
