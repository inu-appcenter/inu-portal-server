package kr.inuappcenterportal.inuportal.domain.course.controller;

import kr.inuappcenterportal.inuportal.domain.course.crawler.excel.LegacyCourseExcelImporter;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingOptionsResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Parameter;
import kr.inuappcenterportal.inuportal.domain.agent.openapi.annotation.AgentExposed;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/course-offerings")
public class CourseOfferingController implements CourseOfferingApiSpecification {

    private static final int COURSE_OFFERING_PAGE_SIZE = 50;

    private final CourseOfferingSyncService courseOfferingSyncService;
    private final CourseOfferingService courseOfferingService;
    private final LegacyCourseExcelImporter legacyCourseExcelImporter;

    @PostMapping(
            value = "/legacy",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResponseDto<Void>> importLegacyCourse(
            @RequestPart("files") List<MultipartFile> files
    ) {
        legacyCourseExcelImporter.importArchive(files);
        return ResponseEntity.ok(ResponseDto.of(null, "과거 강의 데이터 적재 성공"));
    }

    @PostMapping("/sync")
    public ResponseEntity<ResponseDto<Void>> syncCourseOffering(
            @RequestParam int year,
            @RequestParam String modDate
    ) {
        courseOfferingSyncService.syncCourseWithSchoolApi(year, modDate);
        return ResponseEntity.ok(ResponseDto.of(null, "개설 강의 동기화 성공"));
    }

    @AgentExposed(
            name = "course_offerings",
            description = "인천대학교 학기별/학과별 개설 강의 목록 및 수업 정보(과목명, 학점, 이수구분, 요일/시간, 강의실 등)를 조건별로 검색합니다.",
            capabilities = {"개설 강의 목록 검색", "학과별 개설 과목 조회", "학년별 강의 탐색"},
            triggerExamples = {"컴퓨터공학부 2학년 개설강의 뭐 있어?", "이번 학기 데이터사이언스 수업 목록 알려줘", "경영학부 전필 과목 조회해줘"},
            cardTitle = "개설 강의 조회 결과",
            requiresLogin = false
    )
    @GetMapping
    public ResponseEntity<ResponseDto<Page<CourseOfferingResponseDto>>> getCourseOfferings(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "조회 학년도 (예: 2026, 4자리 연도)", required = true)
            @RequestParam Integer year,
            @Parameter(description = "학기 구분 (FIRST: 1학기, SECOND: 2학기, SUMMER: 여름계절학기, WINTER: 겨울계절학기. 1/2학기 숫자 및 한글 문자열도 자동 변환 수용)", required = true)
            @RequestParam SemesterTerm term,
            @Parameter(description = "개설 학과/학부 공식 명칭 (예: 컴퓨터공학부, 경영학부, 데이터과학과, 전기공학과 등). 사용자가 '컴공', '데사' 등 약칭을 사용할 경우 반드시 공식 명칭으로 변환해야 하며, 불확실할 경우 API_GET_COURSE_OPTIONS를 먼저 조회하여 공식 학과명을 확인하세요.")
            @RequestParam(required = false) String deptName,
            @Parameter(description = "단과대학 공식 명칭 (예: 공과대학, 경영대학, 정보기술대학 등)")
            @RequestParam(required = false) String collegeName,
            @Parameter(description = "수강 대상 학년 필터 (예: ['1'], ['2'], ['3'], ['4']). 특정 학년 수업만 조회할 때 전달")
            @RequestParam(required = false) List<String> hyNames,
            @Parameter(description = "이수구분 필터 (예: ['전필'], ['전선'], ['교양'] 등)")
            @RequestParam(required = false) List<String> isuNames,
            @RequestParam(required = false) List<String> isuFldNames,
            @RequestParam(required = false) List<String> ssupTypeNames,
            @Parameter(description = "학점 필터 (예: [2, 3])")
            @RequestParam(required = false) List<Integer> credits,
            @Parameter(description = "과목명 검색 키워드 (학과명이 아닌 순수 교과목 명칭 키워드)")
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MeetingFilterMode meetingFilterMode,
            @RequestParam(required = false) List<String> meetings,
            @Parameter(description = "정렬 기준 (DEFAULT, SAVED_COUNT_DESC, SAVED_COUNT_ASC)")
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

    @AgentExposed(
            name = "get_course_options",
            description = "현재 학기 정보(연도/학기), 전체 개설 학과 목록(코드 및 공식 명칭), 이수구분 목록(전필/전선/교양 등), 융합전공 등 개설 강의 검색에 사용할 수 있는 공식 옵션 값 전체를 조회합니다. 사용자의 학과 표현이 모호하거나 유효한 학과명을 먼저 확인해야 할 때 선행 호출합니다.",
            capabilities = {"개설 학과 공식 목록 확인", "이수구분 옵션 조회", "현재 학기 확인"},
            triggerExamples = {"학교에 무슨 학과가 있어?", "개설강의 검색 옵션 보여줘"},
            cardTitle = "개설 강의 검색 옵션",
            requiresLogin = false
    )
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
