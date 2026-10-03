package kr.inuappcenterportal.inuportal.domain.agent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.agent.dto.CampusWatchCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.agent.dto.CampusWatchJobDto;
import kr.inuappcenterportal.inuportal.domain.agent.service.CampusWatchService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Agent Campus Watch API", description = "스마트 캠퍼스 실시간 빈자리/취소표 감시 및 관리 API")
@RestController
@RequestMapping("/api/v1/agent/watch-jobs")
@RequiredArgsConstructor
public class CampusWatchController {

    private final CampusWatchService campusWatchService;

    @Operation(summary = "스마트 감시 작업 등록 (힐링존/열람실 빈자리 등)")
    @PostMapping
    public ResponseEntity<ResponseDto<CampusWatchJobDto>> registerWatch(
            @AuthenticationPrincipal Member member,
            @RequestBody CampusWatchCreateRequestDto req
    ) {
        CampusWatchJobDto dto = campusWatchService.registerWatch(member, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(dto, "감시 작업이 등록되었습니다."));
    }

    @Operation(summary = "내 스마트 감시 작업 목록 조회")
    @GetMapping
    public ResponseEntity<ResponseDto<List<CampusWatchJobDto>>> getMyWatchJobs(@AuthenticationPrincipal Member member) {
        List<CampusWatchJobDto> list = campusWatchService.getMyWatchJobs(member);
        return ResponseEntity.ok(ResponseDto.of(list, "감시 작업 목록을 조회했습니다."));
    }

    @Operation(summary = "스마트 감시 작업 취소")
    @DeleteMapping("/{jobId}")
    public ResponseEntity<ResponseDto<Void>> cancelWatchJob(
            @AuthenticationPrincipal Member member,
            @PathVariable Long jobId
    ) {
        campusWatchService.cancelWatchJob(member, jobId);
        return ResponseEntity.ok(ResponseDto.of(null, "감시 작업이 취소되었습니다."));
    }
}
