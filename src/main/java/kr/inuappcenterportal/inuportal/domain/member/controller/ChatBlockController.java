package kr.inuappcenterportal.inuportal.domain.member.controller;

import kr.inuappcenterportal.inuportal.domain.member.dto.BlockResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.service.BlockService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/blocks")
public class ChatBlockController implements ChatBlockApiSpecification {

    private final BlockService blockService;

    @PostMapping("/{targetMemberId}")
    public ResponseEntity<ResponseDto<Void>> blockUser(@PathVariable Long targetMemberId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        blockService.blockUser(memberId, targetMemberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @PostMapping("/by-post/{postId}")
    public ResponseEntity<ResponseDto<Void>> blockUserByPostId(@PathVariable Long postId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        blockService.blockUserByPostId(memberId, postId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @PostMapping("/by-reply/{replyId}")
    public ResponseEntity<ResponseDto<Void>> blockUserByReplyId(@PathVariable Long replyId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        blockService.blockUserByReplyId(memberId, replyId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<ResponseDto<Void>> unblockUser(@PathVariable Long targetMemberId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        blockService.unblockUser(memberId, targetMemberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<BlockResponseDto>>> getBlockList(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(blockService.getBlockList(memberId)));
    }
}
