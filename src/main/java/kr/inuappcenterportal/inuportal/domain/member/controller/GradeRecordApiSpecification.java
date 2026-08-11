package kr.inuappcenterportal.inuportal.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordSaveRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "GradeRecord", description = "성적 기록 관련 API")
public interface GradeRecordApiSpecification {

    @Operation(
            summary = "내 성적 전체 조회",
            description = """
                    로그인한 사용자의 모든 성적 기록을 조회합니다.
                    <br><br>
                    성적이 아직 발표되지 않은 과목은 grade가 null로 반환됩니다.
                    <br>
                    재수강 성적 취소 과목은 isCourseRepetition 값으로 true가 반환되고, 아닌 경우 false로 반환됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "내 성적 전체 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "내 성적 전체 조회 응답 예시",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "year": 2026,
                                                  "term": "FIRST",
                                                  "courseCode": "IAA6018",
                                                  "title": "운영체제",
                                                  "credit": 3,
                                                  "grade": "B_PLUS",
                                                  "grade_value": "B+",
                                                  "isMajor": true,
                                                  "isCourseRepetition": false
                                                },
                                                {
                                                  "id": 2,
                                                  "year": 2026,
                                                  "term": "FIRST",
                                                  "courseCode": "0005061",
                                                  "title": "대학영어회화2",
                                                  "credit": 1,
                                                  "grade": "C_PLUS",
                                                  "grade_value": "C+",
                                                  "isMajor": false,
                                                  "isCourseRepetition": true
                                                }
                                              ],
                                              "msg": "내 성적 전체 조회 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<List<GradeRecordResponseDto>>> getAllGradeRecord(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member
    );

    @Operation(
            summary = "특정 학기 내 성적 조회",
            description = """
                    로그인한 사용자의 특정 년도/학기 성적 기록을 조회합니다.
                    <br><br>
                    term은 FIRST, SUMMER, SECOND, WINTER 중 하나를 입력합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "특정 학기 내 성적 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "특정 학기 내 성적 조회 응답 예시",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "year": 2026,
                                                  "term": "FIRST",
                                                  "courseCode": "IAA6018",
                                                  "title": "운영체제",
                                                  "credit": 3,
                                                  "grade": "B_PLUS",
                                                  "grade_value": "B+",
                                                  "isMajor": true,
                                                  "isCourseRepetition": false
                                                }
                                              ],
                                              "msg": "2026/FIRST 내 성적 조회 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값이 올바르지 않거나 존재하지 않는 학기입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<List<GradeRecordResponseDto>>> getGradeRecord(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "year",
                    description = "조회할 년도",
                    example = "2026"
            )
            @RequestParam Integer year,
            @Parameter(
                    name = "term",
                    description = "조회할 학기",
                    schema = @Schema(
                            implementation = SemesterTerm.class,
                            allowableValues = {"FIRST", "SUMMER", "SECOND", "WINTER"},
                            example = "FIRST"
                    )
            )
            @RequestParam SemesterTerm term
    );

    @Operation(
            summary = "내 성적 저장 및 교체",
            description = """
                    로그인한 사용자의 특정 년도/학기 성적을 저장합니다.
                    <br><br>
                    같은 년도/학기의 기존 성적은 모두 삭제한 뒤 요청한 records 값으로 교체합니다.
                    <br>
                    grade는 A+, A0, B+, B0, C+, C0, D+, D0, F, P, NP 중 하나를 입력하며, 성적 미발표 과목은 null로 입력합니다.
                    <br>
                    isCourseRepetition은 재수강 성적 취소 과목이면 true, 아니면 false로 입력합니다.
                    <br>
                    courseCode와 매칭되는 강의가 없어도 성적 기록은 저장됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "내 성적 저장 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "내 성적 저장 응답 예시",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "year": 2026,
                                                  "term": "FIRST",
                                                  "courseCode": "IAA6018",
                                                  "title": "운영체제",
                                                  "credit": 3,
                                                  "grade": "B_PLUS",
                                                  "grade_value": "B+",
                                                  "isMajor": true,
                                                  "isCourseRepetition": false
                                                },
                                                {
                                                  "id": 2,
                                                  "year": 2026,
                                                  "term": "FIRST",
                                                  "courseCode": "0005061",
                                                  "title": "대학영어회화2",
                                                  "credit": 1,
                                                  "grade": "C_PLUS",
                                                  "grade_value": "C+",
                                                  "isMajor": false,
                                                  "isCourseRepetition": true
                                                }
                                              ],
                                              "msg": "내 성적 저장 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값이 올바르지 않거나 존재하지 않는 학기입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<List<GradeRecordResponseDto>>> upsertGradeRecord(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GradeRecordSaveRequestDto.class),
                            examples = @ExampleObject(
                                    name = "내 성적 저장 요청 예시",
                                    value = """
                                            {
                                              "year": 2026,
                                              "term": "FIRST",
                                              "records": [
                                                {
                                                  "courseCode": "IAA6018",
                                                  "title": "운영체제",
                                                  "credit": 3,
                                                  "grade": "B+",
                                                  "isMajor": true,
                                                  "isCourseRepetition": false
                                                },
                                                {
                                                  "courseCode": "0005061",
                                                  "title": "대학영어회화2",
                                                  "credit": 1,
                                                  "grade": "C+",
                                                  "isMajor": false,
                                                  "isCourseRepetition": true
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody GradeRecordSaveRequestDto request
    );

    @Operation(
            summary = "내 성적 개별 수정",
            description = """
                    로그인한 사용자의 특정 성적 기록을 수정합니다.
                    <br><br>
                    요청 본문에 포함된 필드만 수정합니다.
                    <br>
                    grade는 A+, A0, B+, B0, C+, C0, D+, D0, F, P, NP 중 하나를 입력하며, 성적 미발표 과목은 null로 입력합니다.
                    <br>
                    isCourseRepetition은 재수강 성적 취소 과목이면 true, 아니면 false로 입력합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "내 성적 개별 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "내 성적 개별 수정 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 1,
                                                "year": 2026,
                                                "term": "FIRST",
                                                "courseCode": "IAA6018",
                                                "title": "운영체제",
                                                "credit": 3,
                                                "grade": "A_PLUS",
                                                "grade_value": "A+",
                                                "isMajor": true,
                                                "isCourseRepetition": false
                                              },
                                              "msg": "내 성적 저장 및 수정 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값이 올바르지 않습니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 성적 기록입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<GradeRecordResponseDto>> updateGradeRecord(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "gradeRecordId",
                    description = "수정할 성적 기록 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long gradeRecordId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GradeRecordUpdateRequestDto.class),
                            examples = @ExampleObject(
                                    name = "내 성적 개별 수정 요청 예시",
                                    value = """
                                            {
                                              "credit": 3,
                                              "grade": "A+",
                                              "isMajor": true,
                                              "isCourseRepetition": false
                                            }
                                            """
                            )
                    )
            )
            @Valid @RequestBody GradeRecordUpdateRequestDto request
    );

    @Operation(
            summary = "특정 학기 내 성적 전체 삭제",
            description = """
                    로그인한 사용자의 특정 년도/학기 성적 기록을 모두 삭제합니다.
                    <br><br>
                    term은 FIRST, SUMMER, SECOND, WINTER 중 하나를 입력합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "특정 학기 내 성적 전체 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "특정 학기 내 성적 전체 삭제 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "2026/FIRST 전체 성적 삭제 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값이 올바르지 않거나 존재하지 않는 학기입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<Void>> deleteAllGradeRecord(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "year",
                    description = "삭제할 년도",
                    example = "2026"
            )
            @RequestParam Integer year,
            @Parameter(
                    name = "term",
                    description = "삭제할 학기",
                    schema = @Schema(
                            implementation = SemesterTerm.class,
                            allowableValues = {"FIRST", "SUMMER", "SECOND", "WINTER"},
                            example = "FIRST"
                    )
            )
            @RequestParam SemesterTerm term
    );

    @Operation(
            summary = "내 성적 개별 삭제",
            description = "로그인한 사용자의 특정 성적 기록을 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "내 성적 개별 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "내 성적 개별 삭제 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "성적 삭제 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 성적 기록입니다.",
                    content = @Content(schema = @Schema(implementation = ResponseDto.class))
            )
    })
    ResponseEntity<ResponseDto<Void>> deleteGradeRecord(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "gradeRecordId",
                    description = "삭제할 성적 기록 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long gradeRecordId
    );
}
