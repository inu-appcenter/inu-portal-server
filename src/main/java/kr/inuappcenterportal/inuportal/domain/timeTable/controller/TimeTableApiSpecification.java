package kr.inuappcenterportal.inuportal.domain.timeTable.controller;

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
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableNameUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTable.TimeTableVisibilityUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timtable.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "TimeTable", description = "시건표 관련 API")
public interface TimeTableApiSpecification {


    @Operation(
            summary = "내 시간표 상세 조회",
            description = """
                    로그인한 사용자가 소유한 시간표의 상세 정보를 조회합니다.
                    <br><br>
                    시간표 기본 정보와 시간표에 포함된 모든 요소를 함께 반환합니다.
                    <br>
                    각 시간표 요소는 type에 따라 course 또는 customSchedule 중 하나만 값을 가집니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "시간표 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "내 시간표 상세 조회 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 1,
                                                "timeTableName": "1학기 기본 시간표",
                                                "year": 2026,
                                                "term": "FIRST",
                                                "items": [
                                                  {
                                                    "id": 10,
                                                    "type": "COURSE",
                                                    "memo": "중간고사 중요",
                                                    "course": {
                                                      "courseOfferingId": 3,
                                                      "courseId": 12,
                                                      "title": "웹프로그래밍",
                                                      "professor": "박기석",
                                                      "subjectNumber": "0001421001",
                                                      "credit": "3",
                                                      "meetings": [
                                                        {
                                                          "id": 21,
                                                          "location": "07-415",
                                                          "sequence": 1,
                                                          "day": "TUESDAY",
                                                          "startTime": "09:00",
                                                          "endTime": "10:15"
                                                        }
                                                      ]
                                                    },
                                                    "customSchedule": null
                                                  }
                                                ]
                                              },
                                              "msg": "시간표 상세 조회 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "본인 소유 시간표가 아닙니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "내 시간표 상세 조회 권한 없음",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "해당 시간표에 접근할 권한이 없습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 시간표입니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 없음",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "존재하지 않는 시간표입니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<TimeTableDetailResponseDto>> getTimeTableDetail(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "상세 조회할 시간표 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long timeTableId
    );

    @Operation(
            summary = "친구 대표 시간표 상세 조회",
            description = """
                    친구의 특정 년도/학기 대표 시간표 상세 정보를 조회합니다.
                    <br><br>
                    친구 관계이고 차단 관계가 아니어야 조회할 수 있습니다.
                    <br>
                    대표 시간표는 회원별/학기별로 최대 1개만 존재할 수 있으며, 사용자가 대표 시간표를 삭제한 경우처럼 대표 시간표가 없을 수 있습니다.
                    <br>
                    친구의 해당 학기 대표 시간표가 없으면 조회할 수 없습니다.
                    <br>
                    PUBLIC이면 전체 정보를 반환하고, PROTECTED이면 요일/시작 시간/종료 시간만 반환합니다.
                    <br>
                    PRIVATE 시간표는 조회할 수 없습니다.
                    <br>
                    memo는 공개범위(PUBLIC/PROTECTED)와 무관하게 본인만 볼 수 있는 개인 메모이므로 친구 조회 응답에서는 항상 null로 반환됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "친구 대표 시간표 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "PUBLIC 친구 대표 시간표 상세 조회 응답 예시",
                                            value = """
                                                    {
                                                      "data": {
                                                        "id": 1,
                                                        "timeTableName": "1학기 기본 시간표",
                                                        "year": 2026,
                                                        "term": "FIRST",
                                                        "items": [
                                                          {
                                                            "id": 10,
                                                            "type": "COURSE",
                                                            "memo": null,
                                                            "course": {
                                                              "courseOfferingId": 3,
                                                              "courseId": 12,
                                                              "title": "웹프로그래밍",
                                                              "professor": "박기석",
                                                              "subjectNumber": "0001421001",
                                                              "credit": "3",
                                                              "meetings": [
                                                                {
                                                                  "id": 21,
                                                                  "location": "07-415",
                                                                  "sequence": 1,
                                                                  "day": "TUESDAY",
                                                                  "startTime": "09:00",
                                                                  "endTime": "10:15"
                                                                }
                                                              ]
                                                            },
                                                            "customSchedule": null
                                                          }
                                                        ]
                                                      },
                                                      "msg": "친구 대표 시간표 상세 조회 성공"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "PROTECTED 친구 대표 시간표 상세 조회 응답 예시",
                                            value = """
                                                    {
                                                      "data": {
                                                        "id": 1,
                                                        "timeTableName": "1학기 기본 시간표",
                                                        "year": 2026,
                                                        "term": "FIRST",
                                                        "items": [
                                                          {
                                                            "id": null,
                                                            "type": "COURSE",
                                                            "memo": null,
                                                            "course": {
                                                              "courseOfferingId": null,
                                                              "courseId": null,
                                                              "title": null,
                                                              "professor": null,
                                                              "subjectNumber": null,
                                                              "credit": null,
                                                              "meetings": [
                                                                {
                                                                  "id": null,
                                                                  "location": null,
                                                                  "sequence": null,
                                                                  "day": "TUESDAY",
                                                                  "startTime": "09:00",
                                                                  "endTime": "10:15"
                                                                }
                                                              ]
                                                            },
                                                            "customSchedule": null
                                                          }
                                                        ]
                                                      },
                                                      "msg": "친구 대표 시간표 상세 조회 성공"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "year와 term 중 하나만 입력했거나 존재하지 않는 학기입니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "친구 대표 시간표 상세 조회 요청값 오류",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "년도와 학기는 함께 입력해야합니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "친구가 아니거나 차단 관계이거나, 비공개 시간표입니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "친구 대표 시간표 조회 권한 없음",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "msg": "친구가 아닌 사용자의 시간표를 읽을 수 없습니다."
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "비공개 대표 시간표 조회 실패",
                                            value = """
                                                    {
                                                      "data": null,
                                                      "msg": "비공개된 시간표입니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "친구의 해당 년도/학기 대표 시간표가 존재하지 않습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "친구 대표 시간표 없음",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "존재하지 않는 시간표입니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<TimeTableDetailResponseDto>> getFriendPrimarySemesterTimeTable(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "friendMemberId",
                    description = "대표 시간표를 조회할 친구 회원 id",
                    in = ParameterIn.PATH,
                    example = "2"
            )
            @PathVariable Long friendMemberId,
            @Parameter(
                    description = "조회할 학년도. term과 함께 입력해야 합니다.",
                    example = "2026"
            )
            @RequestParam(required = false) Integer year,
            @Parameter(
                    description = "조회할 학기. year와 함께 입력해야 합니다.",
                    schema = @Schema(
                            allowableValues = {"FIRST", "SUMMER", "SECOND", "WINTER"},
                            example = "FIRST"
                    )
            )
            @RequestParam(required = false) SemesterTerm term
    );


    @Operation(
            summary = "시간표 조회",
            description = """
                    로그인한 사용자의 시간표 목록을 조회합니다.
                    <br><br>
                    year와 term을 둘 다 보내지 않으면 전체 시간표를 조회합니다.
                    <br>
                    year와 term을 함께 보내면 해당 년도/학기의 시간표만 조회합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "시간표 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 조회 응답 예시",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "semesterId": 1,
                                                  "year": 2026,
                                                  "term": "FIRST",
                                                  "timeTableName": "1학기 기본 시간표",
                                                  "isPrimary": true,
                                                  "visibility": "PUBLIC"
                                                },
                                                {
                                                  "id": 2,
                                                  "semesterId": 1,
                                                  "year": 2026,
                                                  "term": "FIRST",
                                                  "timeTableName": "공강 많은 시간표",
                                                  "isPrimary": false,
                                                  "visibility": "PRIVATE"
                                                }
                                              ],
                                              "msg": "시간표 조회 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "year와 term 중 하나만 입력했거나 존재하지 않는 학기입니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 조회 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "년도와 학기는 함께 입력해야 합니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<List<TimeTableResponseDto>>> getTimeTables(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    description = "조회할 학년도. term과 함께 입력해야 합니다.",
                    example = "2026"
            )
            @RequestParam(required = false) Integer year,
            @Parameter(
                    description = "조회할 학기. year와 함께 입력해야 합니다.",
                    schema = @Schema(
                            allowableValues = {"FIRST", "SUMMER", "SECOND", "WINTER"},
                            example = "FIRST"
                    )
            )
            @RequestParam(required = false) SemesterTerm term
    );


    @Operation(
            summary = "학기별 시간표 조회",
            description = "로그인한 사용자의 시간표 중 semesterId에 해당하는 학기의 시간표 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "학기별 시간표(id) 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "학기별 시간표 조회 응답 예시",
                                    value = """
                                            {
                                              "data": [
                                                {
                                                  "id": 1,
                                                  "semesterId": 1,
                                                  "year": 2026,
                                                  "term": "FIRST",
                                                  "timeTableName": "1학기 기본 시간표",
                                                  "isPrimary": true,
                                                  "visibility": "PUBLIC"
                                                }
                                              ],
                                              "msg": "학기별 시간표(id) 조회 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 학기입니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "학기별 시간표 조회 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "해당 학기가 존재하지 않습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<List<TimeTableResponseDto>>> getTimeTablesOfSemester(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "semesterId",
                    description = "조회할 학기의 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long semesterId
    );


    @Operation(
            summary = "시간표 이름 수정",
            description = "로그인한 사용자가 소유한 시간표의 이름을 수정합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "시간표 이름 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 이름 변경 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 1,
                                                "semesterId": 1,
                                                "year": 2026,
                                                "term": "FIRST",
                                                "timeTableName": "수정된 시간표",
                                                "isPrimary": true,
                                                "visibility": "PUBLIC"
                                              },
                                              "msg": "시간표 이름 변경 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "시간표 이름이 비어 있거나, 중복된 이름이거나, 접근 권한이 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 이름 변경 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "이미 같은 이름의 시간표가 존재합니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<TimeTableResponseDto>> setTimeTableName(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "수정할 시간표 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long timeTableId,
            @RequestBody @Valid TimeTableNameUpdateRequestDto request
    );


    @Operation(
            summary = "대표 시간표 변경",
            description = """
                    로그인한 사용자가 소유한 시간표를 해당 학기의 대표 시간표로 설정합니다.
                    <br><br>
                    대표 시간표는 회원별/학기별로 최대 1개만 존재할 수 있습니다.
                    <br>
                    대표 시간표는 반드시 존재해야 하는 값은 아니며, 대표 시간표를 삭제해도 다른 시간표가 자동으로 대표 시간표로 지정되지 않습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "대표 시간표 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "대표 시간표 변경 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 2,
                                                "semesterId": 1,
                                                "year": 2026,
                                                "term": "FIRST",
                                                "timeTableName": "공강 많은 시간표",
                                                "isPrimary": true,
                                                "visibility": "PRIVATE"
                                              },
                                              "msg": "대표 시간표 변경 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 시간표이거나 접근 권한이 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "대표 시간표 변경 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "해당 시간표에 접근할 권한이 없습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<TimeTableResponseDto>> setIsPrimary(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "대표 시간표로 설정할 시간표 id",
                    in = ParameterIn.PATH,
                    example = "2"
            )
            @PathVariable Long timeTableId
    );


    @Operation(
            summary = "시간표 공개범위 수정",
            description = "로그인한 사용자가 소유한 시간표의 공개 범위를 수정합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "시간표 공개 범위 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 공개 범위 변경 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 1,
                                                "semesterId": 1,
                                                "year": 2026,
                                                "term": "FIRST",
                                                "timeTableName": "1학기 기본 시간표",
                                                "isPrimary": true,
                                                "visibility": "PRIVATE"
                                              },
                                              "msg": "시간표 공개 범위 변경 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "공개 범위가 비어 있거나, 존재하지 않는 시간표이거나, 접근 권한이 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 공개 범위 변경 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "공개 범위는 필수입니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<TimeTableResponseDto>> setVisibility(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "공개 범위를 수정할 시간표 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long timeTableId,
            @RequestBody @Valid TimeTableVisibilityUpdateRequestDto request
    );


    @Operation(
            summary = "시간표 삭제",
            description = """
                    로그인한 사용자가 소유한 시간표를 삭제합니다.
                    <br><br>
                    삭제한 시간표가 대표 시간표여도 다른 시간표가 자동으로 대표 시간표로 지정되지 않습니다.
                    <br>
                    따라서 해당 회원의 해당 학기에는 대표 시간표가 없을 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "시간표 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 삭제 응답 예시",
                                    value = """
                                            {
                                              "data": 1,
                                              "msg": "시간표 삭제 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 시간표이거나 접근 권한이 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 삭제 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "존재하지 않는 시간표입니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<Long>> deleteTimeTable(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "삭제할 시간표 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long timeTableId
    );


    @Operation(
            summary = "시간표 생성",
            description = """
                    로그인한 사용자의 특정 학기에 시간표를 생성합니다.
                    <br><br>
                    해당 학기의 첫 시간표이면 편의상 대표 시간표로 생성됩니다.
                    <br>
                    대표 시간표는 반드시 존재해야 하는 값은 아니며, 사용자가 따로 대표 시간표를 설정하지 않으면 자동으로 다시 설정되지 않습니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "시간표 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 생성 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 1,
                                                "semesterId": 1,
                                                "year": 2026,
                                                "term": "FIRST",
                                                "timeTableName": "1학기 기본 시간표",
                                                "isPrimary": true,
                                                "visibility": "PUBLIC"
                                              },
                                              "msg": "시간표 생성 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "시간표 이름이 비어 있거나, 중복된 이름이거나, 존재하지 않는 학기입니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 생성 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "이미 같은 이름의 시간표가 존재합니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<TimeTableResponseDto>> createTimeTable(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "semesterId",
                    description = "시간표를 생성할 학기 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long semesterId,
            @RequestBody @Valid TimeTableCreateRequestDto request
    );

    @Operation(
            summary = "시간표 이미지 AI 인식",
            description = """
                    업로드된 시간표 이미지를 Vision AI(Qwen2.5-VL)로 분석하여 강의 목록을 추출하고,
                    해당 학기 개설 강좌 DB와 매칭하여 후보 강좌 및 최적 추천 강좌를 반환합니다.
                    """
    )
    ResponseEntity<ResponseDto<List<kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableImage.TimeTableImageRecognizeResponseDto>>> recognizeTimeTableImage(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(description = "시간표 이미지 파일")
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @Parameter(description = "시간표 년도 (생략 시 현재 학기)")
            @RequestParam(value = "year", required = false) Integer year,
            @Parameter(description = "시간표 학기 (생략 시 현재 학기)")
            @RequestParam(value = "term", required = false) SemesterTerm term
    );
}
