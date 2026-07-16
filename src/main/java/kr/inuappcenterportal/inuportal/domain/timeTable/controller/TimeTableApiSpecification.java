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
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableNameUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableVisibilityUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "TimeTable", description = "TimeTable 관련 API")
public interface TimeTableApiSpecification {


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
            description = "로그인한 사용자가 소유한 시간표를 해당 학기의 대표 시간표로 설정합니다."
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
            description = "로그인한 사용자가 소유한 시간표를 삭제합니다."
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
            description = "로그인한 사용자의 특정 학기에 시간표를 생성합니다. 해당 학기의 첫 시간표이면 대표 시간표로 생성됩니다."
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
}
