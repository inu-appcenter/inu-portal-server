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
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem.TimeTableCourseItemRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem.TimeTableCustomItemRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableItem.TimeTableItemResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "TimeTableItem", description = "시간표 요소 관련 API")
public interface TimeTableItemApiSpecification {

    @Operation(
            summary = "강의 시간표 요소 생성",
            description = """
                    로그인한 사용자가 소유한 시간표에 강의 기반 시간표 요소를 추가합니다.
                    <br><br>
                    생성 응답은 시간표 요소의 최소 정보만 반환합니다.
                    화면 갱신에 필요한 강의명, 강의 시간 등 상세 정보는 시간표 상세 조회 API를 다시 호출해 조회합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "강의 시간표 요소 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "강의 시간표 요소 생성 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 10,
                                                "type": "COURSE",
                                                "memo": "중간고사 중요"
                                              },
                                              "msg": "강의 시간표 요소 생성 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 시간표/강의이거나, 시간표와 강의 학기가 일치하지 않거나, 접근 권한이 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "강의 시간표 요소 생성 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "시간표의 학기와 강의의 학기가 일치하지 않습니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<TimeTableItemResponseDto>> createTimeTableItemForCourse(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "강의 요소를 추가할 시간표 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long timeTableId,
            @RequestBody @Valid TimeTableCourseItemRequestDto request
    );

    @Operation(
            summary = "커스텀 일정 시간표 요소 생성",
            description = """
                    로그인한 사용자가 소유한 시간표에 직접 입력한 커스텀 일정 기반 시간표 요소를 추가합니다.
                    <br><br>
                    요청 본문에는 커스텀 일정 제목, 메모, 하나 이상의 시간 정보를 포함합니다.
                    startTime, endTime은 HH:mm 문자열 형식으로 전달합니다. 예: "09:00"
                    <br><br>
                    생성 응답은 시간표 요소의 최소 정보만 반환합니다.
                    생성된 일정의 제목과 시간 목록은 시간표 상세 조회 API에서 확인합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "커스텀 일정 시간표 요소 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "커스텀 일정 시간표 요소 생성 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 37,
                                                "type": "CUSTOM",
                                                "memo": "스터디룸 예약"
                                              },
                                              "msg": "커스텀 일정 시간표 요소 생성 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값이 올바르지 않거나, 존재하지 않는 시간표이거나, 접근 권한이 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "커스텀 일정 시간표 요소 생성 실패 응답 예시",
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
    ResponseEntity<ResponseDto<TimeTableItemResponseDto>> createTimeTableItemForCustom(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "커스텀 일정 요소를 추가할 시간표 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long timeTableId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TimeTableCustomItemRequestDto.class),
                            examples = @ExampleObject(
                                    name = "커스텀 일정 생성 요청 예시",
                                    value = """
                                            {
                                              "title": "알고리즘 스터디",
                                              "memo": "스터디룸 예약",
                                              "meetings": [
                                                {
                                                  "location": "07-504",
                                                  "day": "MONDAY",
                                                  "startTime": "09:00",
                                                  "endTime": "10:15"
                                                },
                                                {
                                                  "location": "12-402",
                                                  "day": "WEDNESDAY",
                                                  "startTime": "13:30",
                                                  "endTime": "14:45"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid TimeTableCustomItemRequestDto request
    );

    @Operation(
            summary = "커스텀 일정 시간표 요소 수정",
            description = """
                    로그인한 사용자가 소유한 시간표에 포함된 커스텀 일정 시간표 요소를 수정합니다.
                    <br><br>
                    커스텀 일정 제목, 메모, 시간 목록을 요청값으로 교체합니다.
                    startTime, endTime은 HH:mm 문자열 형식으로 전달합니다. 예: "09:00"
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "커스텀 일정 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "커스텀 일정 수정 응답 예시",
                                    value = """
                                            {
                                              "data": {
                                                "id": 37,
                                                "type": "CUSTOM",
                                                "memo": "장소 변경됨"
                                              },
                                              "msg": "커스텀 일정 수정 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 시간표/시간표 요소이거나, 커스텀 일정이 아니거나, 접근 권한이 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "커스텀 일정 수정 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "해당 요소는 커스텀 일정이 아닙니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<TimeTableItemResponseDto>> updateTimeTableItemForCustom(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "수정할 커스텀 일정 요소가 속한 시간표 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long timeTableId,
            @Parameter(
                    name = "customScheduleId",
                    description = "수정할 커스텀 일정 id",
                    in = ParameterIn.PATH,
                    example = "7"
            )
            @PathVariable Long customScheduleId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = TimeTableCustomItemRequestDto.class),
                            examples = @ExampleObject(
                                    name = "커스텀 일정 수정 요청 예시",
                                    value = """
                                            {
                                              "memo": "장소 변경됨",
                                              "title": "알고리즘 스터디",
                                              "meetings": [
                                                {
                                                  "location": "07-415",
                                                  "day": "TUESDAY",
                                                  "startTime": "15:00",
                                                  "endTime": "16:30"
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid TimeTableCustomItemRequestDto request
    );

    @Operation(
            summary = "시간표 요소 삭제",
            description = """
                    로그인한 사용자가 소유한 시간표에 포함된 시간표 요소를 삭제합니다.
                    <br><br>
                    커스텀 일정 기반 요소를 삭제하면 해당 커스텀 일정 데이터도 함께 삭제됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "시간표 요소 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 요소 삭제 응답 예시",
                                    value = """
                                            {
                                              "data": 10,
                                              "msg": "시간표 요소 삭제 성공"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "존재하지 않는 시간표/시간표 요소이거나, 해당 시간표에 속한 요소가 아니거나, 접근 권한이 없습니다.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(
                                    name = "시간표 요소 삭제 실패 응답 예시",
                                    value = """
                                            {
                                              "data": null,
                                              "msg": "해당 시간표에 속한 요소가 아닙니다."
                                            }
                                            """
                            )
                    )
            )
    })
    ResponseEntity<ResponseDto<Long>> deleteTimeTableItem(
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,
            @Parameter(
                    name = "timeTableId",
                    description = "삭제할 요소가 속한 시간표 id",
                    in = ParameterIn.PATH,
                    example = "1"
            )
            @PathVariable Long timeTableId,
            @Parameter(
                    name = "timeTableItemId",
                    description = "삭제할 시간표 요소 id",
                    in = ParameterIn.PATH,
                    example = "10"
            )
            @PathVariable Long timeTableItemId
    );
}
