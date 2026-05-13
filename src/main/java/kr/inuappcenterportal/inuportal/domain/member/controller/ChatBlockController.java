package kr.inuappcenterportal.inuportal.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.member.dto.BlockResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.service.BlockService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Block", description = "차단 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blocks")
public class ChatBlockController {

    private final BlockService blockService;

    @Operation(summary = "유저 차단")
    @SecurityRequirement(name = "Auth")
    @PostMapping("/{targetMemberId}")
    public ResponseEntity<ResponseDto<Void>> blockUser(@PathVariable Long targetMemberId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        blockService.blockUser(memberId, targetMemberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "차단 해제")
    @SecurityRequirement(name = "Auth")
    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<ResponseDto<Void>> unblockUser(@PathVariable Long targetMemberId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        blockService.unblockUser(memberId, targetMemberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "차단 목록 조회")
    @SecurityRequirement(name = "Auth")
    @GetMapping
    public ResponseEntity<ResponseDto<List<BlockResponseDto>>> getBlockList(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(blockService.getBlockList(memberId)));
    }
}
