package kr.inuappcenterportal.inuportal.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendAliasRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.service.FriendService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friend", description = "친구 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 신청 (닉네임 기반)")
    @SecurityRequirement(name = "Auth")
    @PostMapping("/request")
    public ResponseEntity<ResponseDto<Void>> requestFriend(@Valid @RequestBody FriendRequestDto requestDto, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        friendService.requestFriend(memberId, requestDto);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "대기 중인 친구 요청 목록 조회")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/pending")
    public ResponseEntity<ResponseDto<List<FriendResponseDto>>> getPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.getPendingRequests(memberId)));
    }

    @Operation(summary = "내가 보낸 대기 중인 친구 요청 목록 조회")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/pending/sent")
    public ResponseEntity<ResponseDto<List<FriendResponseDto>>> getSentPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.getSentPendingRequests(memberId)));
    }

    @Operation(summary = "친구 검색 (닉네임 기반)")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/search")
    public ResponseEntity<ResponseDto<FriendResponseDto>> searchFriend(@RequestParam String nickname, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.searchMemberByNickname(memberId, nickname)));
    }

    @Operation(summary = "친구 요청 수락")
    @SecurityRequirement(name = "Auth")
    @PostMapping("/{friendId}/accept")
    public ResponseEntity<ResponseDto<Void>> acceptFriend(@PathVariable Long friendId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        friendService.acceptFriend(memberId, friendId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "친구 삭제/요청 거절")
    @SecurityRequirement(name = "Auth")
    @DeleteMapping("/{friendId}")
    public ResponseEntity<ResponseDto<Void>> deleteFriend(@PathVariable Long friendId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        friendService.deleteFriend(memberId, friendId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "친구 목록 조회")
    @SecurityRequirement(name = "Auth")
    @GetMapping
    public ResponseEntity<ResponseDto<List<FriendResponseDto>>> getFriendList(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.getFriendList(memberId)));
    }

    @Operation(summary = "친구 별명 설정")
    @SecurityRequirement(name = "Auth")
    @PatchMapping("/{friendId}/alias")
    public ResponseEntity<ResponseDto<Void>> updateFriendAlias(
            @PathVariable Long friendId,
            @Valid @RequestBody FriendAliasRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        friendService.updateFriendAlias(memberId, friendId, requestDto);
        return ResponseEntity.ok(ResponseDto.of(null));
    }
}
