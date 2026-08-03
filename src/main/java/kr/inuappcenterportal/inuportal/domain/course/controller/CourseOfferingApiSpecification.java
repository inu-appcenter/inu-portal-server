package kr.inuappcenterportal.inuportal.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseMeeting.MeetingFilterMode;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "CourseOffering", description = "개설 강의 관련 API")
public interface CourseOfferingApiSpecification {

    @Operation(
            summary = "개설 강의 및 강의 시간 동기화",
            description = """
                    [관리자 전용] 학교 API에서 개설 강의와 강의 시간 정보를 조회하여 DB에 동기화합니다.
                    
                    동기화 순서:
                    1. 학교 개설 강의 API(A_MAP_COURSE_INFO)를 호출하여 Course/CourseOffering을 생성 또는 갱신합니다.
                    2. 학교 강의 시간 API(A_MAP_COURSE_TIMETABLE)를 호출하여 CourseMeeting을 갱신합니다.
                    
                    CourseOffering은 YEAR + TERM_CODE + HAKSU_CODE 기준으로 식별합니다.
                    CourseMeeting은 기존 시간을 삭제한 뒤 최신 API 응답 기준으로 다시 저장합니다.
                    강의 시간 API에만 존재하고 매칭되는 CourseOffering이 없는 시간 정보는 로그를 남기고 스킵합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "개설 강의 동기화 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "개설 강의 동기화 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "개설 강의 동기화 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 강의/개설 강의 또는 잘못된 요청 값입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한이 없습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "동기화 대상 학기를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<Void>> syncCourseOffering(
            @Parameter(
                    description = "동기화할 강의 데이터의 기준 연도입니다. 학교 API의 YEAR 파라미터로 전달됩니다.",
                    example = "2026",
                    required = true
            )
            @RequestParam int year,

            @Parameter(
                    description = "학교 API의 MOD_DATE 파라미터입니다. 보통 yyyyMMdd 형식으로 전달합니다.",
                    example = "20260101",
                    required = true
            )
            @RequestParam String modDate
    );

    @Operation(
            summary = "학기별 개설 강의 목록 조회",
            description = """
                    년도와 학기 기준으로 개설 강의 목록을 페이지네이션하여 조회합니다.
                    
                    year와 term은 필수이며, 모든 검색은 해당 학기의 개설 강의 안에서만 수행됩니다.
                    필터 파라미터는 한글 표시명(xxxName)을 전달합니다.
                    hyNames, isuNames, isuFldNames, ssupTypeNames, credits는 다중 선택 필터입니다.
                    다중 선택은 같은 query parameter를 반복해서 전달합니다. 예: hyNames=2&hyNames=3&credits=2&credits=3
                    시간대 필터는 meetingFilterMode와 meetings를 함께 사용합니다.
                    meetingFilterMode가 HAS_CLASS이면 선택한 요일/시간대에 수업이 있는 개설 강의를 조회합니다.
                    meetingFilterMode가 NO_CLASS이면 선택한 요일/시간대에 수업이 없는 개설 강의를 조회합니다.
                    meetings는 DAY,startTime,endTime 형식으로 전달하며, 여러 개 선택 시 같은 query parameter를 반복합니다.
                    예: meetingFilterMode=HAS_CLASS&meetings=MONDAY,10:00,12:00&meetings=WEDNESDAY,13:00,15:00
                    meetings가 있고 meetingFilterMode가 없으면 기본값은 HAS_CLASS입니다.
                    서버 내부에서는 전달된 한글 표시명을 enum code로 변환하여 검색합니다.
                    응답에는 CourseOffering 정보와 해당 개설 강의의 CourseMeeting 목록이 포함됩니다.
                    CourseOffering의 id는 시간표 요소 생성 시 courseOfferingId로 사용합니다.
                    xxxCode 필드는 서버 enum의 name() 값이며, xxxName 필드는 사용자에게 표시할 한글명입니다.
                    온라인 강의 또는 시간 미정 강의는 CourseMeeting 목록이 빈 배열일 수 있습니다.
                    온라인 강의 또는 시간 미정 강의는 HAS_CLASS 시간대 필터에서는 제외되고, NO_CLASS 시간대 필터에서는 포함될 수 있습니다.
                    page는 0부터 시작하며, 페이지 크기는 50으로 고정됩니다.
                    """
    )
    @Parameters({
            @Parameter(
                    name = "page",
                    in = ParameterIn.QUERY,
                    description = "페이지 번호입니다. 0부터 시작합니다.",
                    schema = @Schema(type = "integer", defaultValue = "0", minimum = "0"),
                    example = "0"
            )
    })
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "개설 강의 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "개설 강의 목록 조회 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "content": [
                                                  {
                                                    "id": 10,
                                                    "syllabus": null,
                                                    "subjectNumber": "2000259001",
                                                    "professor": null,
                                                    "courseId": 10,
                                                    "courseCode": "2000259",
                                                    "courseTitle": "영어회화",
                                                    "courseTitleEng": "English Conversation",
                                                    "semesterId": 3,
                                                    "year": 2026,
                                                    "term": "SECOND",
                                                    "termName": "2학기",
                                                    "cnctrIsuCode": "NORMAL",
                                                    "cnctrIsuName": "일반(1~15주)",
                                                    "deptCode": "ENGLISH_LANGUAGE_LITERATURE",
                                                    "deptName": "영어영문학과",
                                                    "collegeCode": "HUMANITIES",
                                                    "collegeName": "인문대학",
                                                    "isuFldCode": "MAJOR_ADVANCED",
                                                    "isuFldName": "전공심화",
                                                    "isuCode": "MAJOR_ADVANCED",
                                                    "isuName": "전공심화",
                                                    "ssupTypeCode": "LANGUAGE_THEORY",
                                                    "ssupTypeName": "이론(어학)",
                                                    "hyCode": "GRADE2",
                                                    "hyName": "2",
                                                    "englishCode": "UN_TARGET",
                                                    "englishName": "비대상",
                                                    "credit": 3,
                                                    "capacity": null,
                                                    "enrolledCount": null,
                                                    "note": null,
                                                    "meetings": [
                                                      {
                                                        "id": 101,
                                                        "location": "제15호관 인문대학-403 전용어학실습실-3",
                                                        "sequence": "야3",
                                                        "day": "TUESDAY",
                                                        "startTime": "19:50",
                                                        "endTime": "20:40"
                                                      }
                                                    ]
                                                  }
                                                ],
                                                "totalElements": 1,
                                                "totalPages": 1,
                                                "size": 50,
                                                "number": 0
                                              },
                                              "msg": "개설 강의 목록 조회 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값이 올바르지 않습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "조회 대상 학기를 찾을 수 없습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<Page<CourseOfferingResponseDto>>> getCourseOfferings(
            @Parameter(
                    description = "조회할 개설 강의의 연도입니다.",
                    example = "2026",
                    required = true
            )
            @RequestParam Integer year,

            @Parameter(
                    description = "조회할 학기입니다.",
                    schema = @Schema(
                            implementation = SemesterTerm.class,
                            allowableValues = {"FIRST", "SUMMER", "SECOND", "WINTER"}
                    ),
                    example = "SECOND",
                    required = true
            )
            @RequestParam SemesterTerm term,

            @Parameter(
                    description = "학과 필터입니다. 응답의 deptName 값을 전달합니다.",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {
                                    "Global Trade & Service학부",
                                    "HUSS(타대학)",
                                    "HUSS포용사회이니셔티브학부",
                                    "IBE전공",
                                    "건설환경공학전공",
                                    "건축공학전공",
                                    "경영학부",
                                    "경제학과",
                                    "경제학과(야)",
                                    "공연예술학과",
                                    "광전자공학전공(연계)",
                                    "교양",
                                    "교직",
                                    "국어교육과",
                                    "국어국문학과",
                                    "군사학",
                                    "기계공학과",
                                    "나노바이오공학전공",
                                    "데이터과학과",
                                    "도시건축학부",
                                    "도시건축학전공",
                                    "도시공학과",
                                    "도시행정학과",
                                    "도시환경공학부",
                                    "독어독문학과",
                                    "동북아국제통상전공",
                                    "디자인학부",
                                    "무역학부(야)",
                                    "문헌정보학과",
                                    "물류학전공(연계)",
                                    "물리학과",
                                    "미디어커뮤니케이션학과",
                                    "미래교육디자인연계전공",
                                    "미래자동차연계전공",
                                    "바이오-로봇시스템공학과",
                                    "반도체융합전공",
                                    "법학부",
                                    "분자의생명전공",
                                    "불어불문학과",
                                    "사회복지학과",
                                    "산업경영공학과",
                                    "생명공학부",
                                    "생명공학전공",
                                    "생명과학부",
                                    "생명과학전공",
                                    "서양화전공",
                                    "세무회계학과",
                                    "소비자학과",
                                    "소셜데이터사이언스연계전공",
                                    "수학과",
                                    "수학교육과",
                                    "스마트물류공학전공",
                                    "스포츠과학부",
                                    "신소재공학과",
                                    "안전공학과",
                                    "에너지화학공학과",
                                    "역사교육과",
                                    "영어교육과",
                                    "영어영문학과",
                                    "운동건강학부",
                                    "유아교육과",
                                    "윤리교육과",
                                    "인문문화예술기획연계전공",
                                    "일본지역문화학과",
                                    "일선",
                                    "일어교육과",
                                    "임베디드시스템공학과",
                                    "자유전공학부",
                                    "전기공학과",
                                    "전자공학과",
                                    "전자공학부",
                                    "전자공학전공",
                                    "정보통신공학과",
                                    "정치외교학과",
                                    "조형예술학부",
                                    "중어중국학과",
                                    "지능형로봇시스템연계전공",
                                    "창의인재개발학과",
                                    "창의적디자인연계전공",
                                    "체육교육과",
                                    "컴퓨터공학부",
                                    "패션산업학과",
                                    "한국화전공",
                                    "해양학과",
                                    "행정학과",
                                    "화학과",
                                    "환경공학전공"
                            }
                    ),
                    example = "컴퓨터공학부"
            )
            @RequestParam(required = false) String deptName,

            @Parameter(
                    description = "단과대 필터입니다. 응답의 collegeName 값을 전달합니다.",
                    schema = @Schema(
                            type = "string",
                            allowableValues = {
                                    "경영대학",
                                    "공과대학",
                                    "교양",
                                    "교직",
                                    "군사학",
                                    "글로벌정경대학",
                                    "기타",
                                    "단과대구분없음",
                                    "단과대구분없음(법학)",
                                    "도시과학대학",
                                    "사범대학",
                                    "사회과학대학",
                                    "생명과학기술대학",
                                    "예술체육대학",
                                    "융합자유전공대학",
                                    "인문대학",
                                    "일선",
                                    "자연과학대학",
                                    "정보기술대학"
                            }
                    ),
                    example = "정보기술대학"
            )
            @RequestParam(required = false) String collegeName,

            @Parameter(
                    description = "대상 학년 필터입니다. 응답의 hyName 값을 전달합니다. 여러 개 선택 가능합니다.",
                    array = @ArraySchema(schema = @Schema(type = "string", allowableValues = {"1", "2", "3", "4", "전학년"})),
                    example = "2"
            )
            @RequestParam(required = false) List<String> hyNames,

            @Parameter(
                    description = "이수 구분 필터입니다. 응답의 isuName 값을 전달합니다. 여러 개 선택 가능합니다.",
                    array = @ArraySchema(schema = @Schema(
                            type = "string",
                            allowableValues = {
                                    "교직",
                                    "군사학",
                                    "기초교양",
                                    "심화교양",
                                    "일반선택",
                                    "전공기초",
                                    "전공심화",
                                    "전공핵심",
                                    "핵심교양"
                            }
                    )),
                    example = "전공심화"
            )
            @RequestParam(required = false) List<String> isuNames,

            @Parameter(
                    description = "이수 영역 필터입니다. 응답의 isuFldName 값을 전달합니다. 여러 개 선택 가능합니다.",
                    array = @ArraySchema(schema = @Schema(
                            type = "string",
                            allowableValues = {
                                    "(핵심)INU세미나",
                                    "(핵심)과학기술",
                                    "(핵심)사회",
                                    "(핵심)예술체육",
                                    "(핵심)외국어",
                                    "(핵심)인문",
                                    "과학기술",
                                    "교직",
                                    "군사학",
                                    "기초과학ㆍ공학",
                                    "사회",
                                    "예술체육",
                                    "외국어",
                                    "인문",
                                    "일반선택",
                                    "전공기초",
                                    "전공심화",
                                    "전공핵심",
                                    "학문의기초"
                            }
                    )),
                    example = "전공심화"
            )
            @RequestParam(required = false) List<String> isuFldNames,

            @Parameter(
                    description = "수업 유형 필터입니다. 응답의 ssupTypeName 값을 전달합니다. 여러 개 선택 가능합니다.",
                    array = @ArraySchema(schema = @Schema(
                            type = "string",
                            allowableValues = {
                                    "K-MOOC",
                                    "RISE(시간표 없음)",
                                    "RISE(시간표 있음)",
                                    "e-Learning",
                                    "e-Learning(HUSS)",
                                    "강의(이론)",
                                    "담장너머~,사회봉사(1)",
                                    "미술실기",
                                    "사회봉사(2)",
                                    "사회봉사(3)",
                                    "실험실습",
                                    "열린사이버대학(OCU)",
                                    "예술체육실기",
                                    "온라인혼합형강좌",
                                    "온라인혼합형강좌(HUSS)",
                                    "이론(어학)",
                                    "이론실험실습",
                                    "자기설계세미나",
                                    "체육실기",
                                    "현장형(HUSS)"
                            }
                    )),
                    example = "이론(어학)"
            )
            @RequestParam(required = false) List<String> ssupTypeNames,

            @Parameter(
                    description = "학점 필터입니다. 여러 개 선택 가능합니다.",
                    array = @ArraySchema(schema = @Schema(type = "integer", allowableValues = {"1", "2", "3", "4"})),
                    example = "3"
            )
            @RequestParam(required = false) List<Integer> credits,

            @Parameter(
                    description = "강의명, 영문명, 학수번호 검색 키워드입니다.",
                    example = "운영체제"
            )
            @RequestParam(required = false) String keyword,

            @Parameter(
                    description = """
                            시간대 필터 모드입니다.
                            HAS_CLASS: 선택한 요일/시간대에 수업이 있는 개설 강의 조회
                            NO_CLASS: 선택한 요일/시간대에 수업이 없는 개설 강의 조회
                            meetings가 있고 meetingFilterMode가 없으면 HAS_CLASS로 동작합니다.
                            """,
                    schema = @Schema(
                            implementation = MeetingFilterMode.class,
                            allowableValues = {"HAS_CLASS", "NO_CLASS"}
                    ),
                    example = "HAS_CLASS"
            )
            @RequestParam(required = false) MeetingFilterMode meetingFilterMode,

            @Parameter(
                    description = """
                            시간대 필터입니다. DAY,startTime,endTime 형식으로 전달합니다.
                            DAY 값은 MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY를 지원합니다.
                            시간은 HH:mm 형식입니다.
                            여러 시간대를 선택하려면 같은 query parameter를 반복합니다.
                            예: meetings=MONDAY,10:00,12:00&meetings=WEDNESDAY,13:00,15:00
                            """,
                    array = @ArraySchema(schema = @Schema(type = "string")),
                    example = "MONDAY,10:00,12:00"
            )
            @RequestParam(required = false) List<String> meetings,

            @Parameter(hidden = true)
            @RequestParam(defaultValue = "0") Integer page
    );
}
