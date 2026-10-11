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
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollSubmitRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollCreateResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollJoinPreviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollMySubmissionResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollParticipantsResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollResultResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollRevisionResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollSubmitResponseDto;
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

    @Operation(
            summary = "내 제출본 조회",
            description = """
                    내가 제출한 가능 시간을 조회합니다.
                    <br>
                    제출한 적이 없으면 404 SUBMISSION_NOT_FOUND입니다. 오류가 아니라 정상 흐름이며, 빈 그리드로 시작하면 됩니다.
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 제출본 조회 성공"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND / SUBMISSION_NOT_FOUND")
    })
    ResponseEntity<ResponseDto<SchedulePollMySubmissionResponseDto>> getMySubmission(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "조율 ID") @PathVariable Long pollId);

    @Operation(
            summary = "제출·재제출(= 참여)",
            description = """
                    가능 시간을 제출합니다. 처음 호출하면 참여자가 되고, 이후에는 덮어씁니다.
                    <br>
                    참여자가 2명 이상이면 결과를 새로 계산해 새 버전을 만들고, 이전 버전은 STALE이 됩니다.
                    <br><br>
                    clientSubmissionId: 제출 시도마다 새 UUID. 같은 시도를 다시 보내면 처음 응답을 그대로 돌려줍니다.
                    <br>
                    baseSubmissionRevision: 입력 화면을 열 때 받은 submissionRevision. 다르면 409 SUBMISSION_CONFLICT. 첫 제출이면 null
                    <br>
                    via = ANYTIME이면 available을 무시하고 모든 열 × 전체 시간대로 저장합니다.
                    <br>
                    판정 순서: 같은 요청 재전송 → 끝남·마감(POLL_CLOSED) → 차단(BLOCKED) → 10명(POLL_FULL) → 다른 기기 수정(SUBMISSION_CONFLICT) → 구간 검증
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "시간 제출 성공"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_SLOT"),
            @ApiResponse(responseCode = "403", description = "BLOCKED"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "POLL_CLOSED / POLL_FULL / SUBMISSION_CONFLICT")
    })
    ResponseEntity<ResponseDto<SchedulePollSubmitResponseDto>> submit(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "조율 ID") @PathVariable Long pollId,
            @Valid @RequestBody SchedulePollSubmitRequestDto request);

    @Operation(
            summary = "참여자 목록",
            description = """
                    참여자(시간을 제출한 사람) 목록을 참여 순으로 조회합니다. 로그인한 사람이면 누구나 볼 수 있습니다.
                    <br>
                    제출하지 않은 개설자는 목록에 없습니다. 학번은 가려서(2024***01) 내려 줍니다.
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참여자 목록 조회 성공"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND")
    })
    ResponseEntity<ResponseDto<SchedulePollParticipantsResponseDto>> getParticipants(
            @Parameter(description = "조율 ID") @PathVariable Long pollId);

    @Operation(
            summary = "최신 결과 조회",
            description = """
                    최신 결과(히트맵, 추천 후보)를 조회합니다. 참여자만 볼 수 있습니다.
                    <br>
                    heatmap[c][r]은 columns[c]의 rowStart + 30분 × r 칸에 가능한 참여자의 비트마스크이며, participants[i].index가 비트 위치입니다.
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결과 조회 성공"),
            @ApiResponse(responseCode = "403", description = "NOT_PARTICIPANT"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "RESULT_NOT_READY(참여자 2명 미만)")
    })
    ResponseEntity<ResponseDto<SchedulePollResultResponseDto>> getLatestResult(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "조율 ID") @PathVariable Long pollId);

    @Operation(
            summary = "특정 버전 결과 조회",
            description = """
                    특정 버전의 결과를 조회합니다. 참여자만 볼 수 있습니다. 버전별 내용은 바뀌지 않으므로 캐시해도 됩니다.
                    <br>
                    (state, isFinal은 바뀔 수 있습니다.) 채팅 결과 카드는 이 경로로 그 버전을 엽니다.
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결과 조회 성공"),
            @ApiResponse(responseCode = "403", description = "NOT_PARTICIPANT"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND(없는 조율 또는 없는 버전)")
    })
    ResponseEntity<ResponseDto<SchedulePollResultResponseDto>> getResult(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "조율 ID") @PathVariable Long pollId,
            @Parameter(description = "결과 버전") @PathVariable int version);

    @Operation(
            summary = "마감 변경·해제·즉시 종료",
            description = """
                    개설자만 할 수 있습니다. 요청은 셋 중 하나입니다.
                    <br>
                    {"deadlineAt": "2026-10-12T23:00:00+09:00"} 마감 변경 / {"deadlineAt": null} 마감 해제 / {"close": true} 즉시 종료
                    <br>
                    두 필드를 함께 보내거나 둘 다 없으면 400 VALIDATION_ERROR입니다.
                    <br><br>
                    종료하면 참여자 2명 이상일 때 최신 결과를 최종 결과로 고정하고 참여자에게 푸시를 보냅니다.
                    <br>
                    마감을 바꾸면 마감 1시간 전 알림을 새 마감 기준으로 다시 보냅니다.
                    <br>
                    응답은 변경 확인(폴링) API와 같은 모양입니다.
                    <br><br>
                    """ + ERROR_DESCRIPTION
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "시간 맞추기 수정 성공"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR / INVALID_RANGE(새 마감이 현재 이전)"),
            @ApiResponse(responseCode = "403", description = "NOT_HOST"),
            @ApiResponse(responseCode = "404", description = "POLL_NOT_FOUND"),
            @ApiResponse(responseCode = "409", description = "POLL_CLOSED")
    })
    ResponseEntity<ResponseDto<SchedulePollRevisionResponseDto>> update(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Parameter(description = "조율 ID") @PathVariable Long pollId,
            @RequestBody SchedulePollUpdateRequestDto request);
}
