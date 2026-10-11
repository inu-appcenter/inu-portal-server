package kr.inuappcenterportal.inuportal.domain.schedulePoll.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollCreateResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollJoinPreviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollRevisionResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "SchedulePoll", description = "시간 맞추기 API")
public interface SchedulePollApiSpecification {

    String ERROR_DESCRIPTION = "오류 응답은 { \"data\": { \"code\": \"…\" }, \"msg\": \"…\" } 형식이며, 클라이언트는 code로 구분한다.";

    @Operation(
            summary = "조율 생성",
            description = """
                    시간 맞추기 조율을 만듭니다. 개설자는 이 시점에 참여자가 아닙니다(시간을 제출해야 참여자).
                    <br><br>
                    정기면 days, 일회성이면 startDate·endDate를 쓰고, 다른 쪽 값은 무시합니다.
                    <br>
                    durationMin: FIXED면 30·60·90 중 하나, AT_LEAST면 30분 단위(시간대 길이 이하).
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "시간 맞추기 생성 성공",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "data": {
                                        "pollId": 812,
                                        "linkToken": "Q2x5bXh0b2dIc2VrVnJ0bW9p",
                                        "linkUrl": "https://intip.inuappcenter.kr/schedule-polls/join/Q2x5bXh0b2dIc2VrVnJ0bW9p",
                                        "revision": 1
                                      },
                                      "msg": "시간 맞추기 생성 성공"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR(필드 누락·형식 오류) / INVALID_RANGE(시간 범위·기간·마감 조건 위반)"),
            @ApiResponse(responseCode = "403", description = "NOT_SHARE_TARGET(참여하지 않은 채팅방)")
    })
    ResponseEntity<ResponseDto<SchedulePollCreateResponseDto>> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Valid @RequestBody SchedulePollCreateRequestDto request);

    @Operation(
            summary = "조율 조회",
            description = """
                    조건, 상태, 내 권한을 조회합니다. 로그인한 사람이면 누구나 조회할 수 있습니다.
                    다른 사람의 입력과 히트맵은 포함하지 않습니다.
                    <br><br>
                    마감 시각이 지났으면 서버의 마감 처리 전이라도 status = CLOSED, closedAt = 마감 시각으로 내려 줍니다.
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "시간 맞추기 조회 성공"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND")
    })
    ResponseEntity<ResponseDto<SchedulePollDetailResponseDto>> get(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "조율 ID") @PathVariable Long pollId);

    @Operation(
            summary = "변경 확인(폴링)",
            description = """
                    화면이 떠 있는 동안 주기적으로 호출해 변경 여부를 확인합니다. Cache-Control: no-store로 응답합니다.
                    <br>
                    응답 revision이 캐시한 값보다 클 때만 참여자 목록·결과를 다시 조회합니다.
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "시간 맞추기 변경 확인 성공"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND")
    })
    ResponseEntity<ResponseDto<SchedulePollRevisionResponseDto>> getRevision(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "조율 ID") @PathVariable Long pollId);

    @Operation(
            summary = "링크 미리보기",
            description = """
                    참여 링크로 들어왔을 때 호출합니다. 비로그인으로도 호출할 수 있고, 참여 처리를 하지 않습니다.
                    <br>
                    canEnter는 화면 안내용이며 최종 판정은 제출 API에서 합니다. 비로그인이면 myStatus는 항상 null입니다.
                    <br>
                    판정 순서: 끝났거나 마감 지남(CLOSED) → 비로그인(입력 가능) → 개설자와 차단 관계(BLOCKED) → 이미 참여자(입력 가능) → 참여자 10명(FULL)
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "시간 맞추기 링크 미리보기 성공"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND(없는 토큰)")
    })
    ResponseEntity<ResponseDto<SchedulePollJoinPreviewResponseDto>> previewByLink(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "참여 링크 토큰") @PathVariable String token);
}
