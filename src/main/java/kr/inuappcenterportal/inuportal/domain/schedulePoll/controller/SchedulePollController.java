package kr.inuappcenterportal.inuportal.domain.schedulePoll.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollSubmitRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollCreateResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollJoinPreviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollMySubmissionResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollParticipantsResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollResultResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollRevisionResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollSubmitResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollResultService;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollService;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollSubmissionService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule-polls")
public class SchedulePollController implements SchedulePollApiSpecification {

    private final SchedulePollService schedulePollService;
    private final SchedulePollSubmissionService schedulePollSubmissionService;
    private final SchedulePollResultService schedulePollResultService;

    @PostMapping
    public ResponseEntity<ResponseDto<SchedulePollCreateResponseDto>> create(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody SchedulePollCreateRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseDto.of(schedulePollService.create(member, request), "시간 맞추기 생성 성공"));
    }

    @GetMapping("/{pollId}")
    public ResponseEntity<ResponseDto<SchedulePollDetailResponseDto>> get(
            @AuthenticationPrincipal Member member,
            @PathVariable Long pollId) {
        return ResponseEntity.ok(ResponseDto.of(schedulePollService.get(member, pollId), "시간 맞추기 조회 성공"));
    }

    @GetMapping("/{pollId}/revision")
    public ResponseEntity<ResponseDto<SchedulePollRevisionResponseDto>> getRevision(
            @AuthenticationPrincipal Member member,
            @PathVariable Long pollId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ResponseDto.of(schedulePollService.getRevision(member, pollId), "시간 맞추기 변경 확인 성공"));
    }

    // 비로그인 허용(SecurityConfig). 비로그인이면 member는 null
    @GetMapping("/join/{token}")
    public ResponseEntity<ResponseDto<SchedulePollJoinPreviewResponseDto>> previewByLink(
            @AuthenticationPrincipal Member member,
            @PathVariable String token) {
        return ResponseEntity.ok(ResponseDto.of(schedulePollService.previewByLink(member, token), "시간 맞추기 링크 미리보기 성공"));
    }

    @GetMapping("/{pollId}/my-submission")
    public ResponseEntity<ResponseDto<SchedulePollMySubmissionResponseDto>> getMySubmission(
            @AuthenticationPrincipal Member member,
            @PathVariable Long pollId) {
        return ResponseEntity.ok(ResponseDto.of(schedulePollSubmissionService.getMine(member, pollId), "내 제출본 조회 성공"));
    }

    @PutMapping("/{pollId}/my-submission")
    public ResponseEntity<ResponseDto<SchedulePollSubmitResponseDto>> submit(
            @AuthenticationPrincipal Member member,
            @PathVariable Long pollId,
            @Valid @RequestBody SchedulePollSubmitRequestDto request) {
        return ResponseEntity.ok(ResponseDto.of(schedulePollSubmissionService.submit(member, pollId, request), "시간 제출 성공"));
    }

    @GetMapping("/{pollId}/participants")
    public ResponseEntity<ResponseDto<SchedulePollParticipantsResponseDto>> getParticipants(
            @PathVariable Long pollId) {
        return ResponseEntity.ok(ResponseDto.of(schedulePollSubmissionService.getParticipants(pollId), "참여자 목록 조회 성공"));
    }

    @GetMapping("/{pollId}/results/latest")
    public ResponseEntity<ResponseDto<SchedulePollResultResponseDto>> getLatestResult(
            @AuthenticationPrincipal Member member,
            @PathVariable Long pollId) {
        return ResponseEntity.ok(ResponseDto.of(schedulePollResultService.getLatest(member, pollId), "결과 조회 성공"));
    }

    @GetMapping("/{pollId}/results/{version}")
    public ResponseEntity<ResponseDto<SchedulePollResultResponseDto>> getResult(
            @AuthenticationPrincipal Member member,
            @PathVariable Long pollId,
            @PathVariable int version) {
        return ResponseEntity.ok(ResponseDto.of(schedulePollResultService.getVersion(member, pollId, version), "결과 조회 성공"));
    }
}
