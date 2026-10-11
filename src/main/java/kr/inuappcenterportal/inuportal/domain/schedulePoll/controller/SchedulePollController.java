package kr.inuappcenterportal.inuportal.domain.schedulePoll.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollCreateResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollJoinPreviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollRevisionResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollService;
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
}
