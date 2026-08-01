package kr.inuappcenterportal.inuportal.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

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
                    
                    응답에는 CourseOffering 정보와 해당 개설 강의의 CourseMeeting 목록이 포함됩니다.
                    page는 0부터 시작하며, 기본 size는 50입니다.
                    """
    )
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
                                                    "syllabus": null,
                                                    "subjectNumber": "2000259001",
                                                    "method": "OFFLINE",
                                                    "professor": null,
                                                    "courseId": 10,
                                                    "courseTitle": "영어회화",
                                                    "semesterId": 3,
                                                    "year": 2026,
                                                    "term": "SECOND",
                                                    "targetDepartment": "ENGLISH",
                                                    "language": "KOREAN",
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

            @ParameterObject
            Pageable pageable
    );
}
