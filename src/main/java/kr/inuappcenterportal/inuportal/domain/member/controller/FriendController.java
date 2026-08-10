package kr.inuappcenterportal.inuportal.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendInviteCodeResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendInvitePreviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendAliasRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberProfileResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.service.FriendInviteService;
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
@SecurityRequirement(name = "Auth")
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;
    private final FriendInviteService friendInviteService;

    @Operation(summary = "친구 신청 (닉네임 기반)")
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

    @Operation(summary = "내 친구추가 링크(초대 코드) 조회", description = "유효한 코드가 있으면 그대로, 없으면 새로 발급해 반환합니다. QR/URL 공유에 사용합니다.")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/invite-code")
    public ResponseEntity<ResponseDto<FriendInviteCodeResponseDto>> getInviteCode(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendInviteService.getOrCreateInviteCode(memberId), "친구추가 링크 조회 성공"));
    }

    @Operation(summary = "내 친구추가 링크 재발급", description = "기존 링크를 즉시 폐기하고 새 코드를 발급합니다.")
    @SecurityRequirement(name = "Auth")
    @PostMapping("/invite-code/refresh")
    public ResponseEntity<ResponseDto<FriendInviteCodeResponseDto>> refreshInviteCode(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendInviteService.refreshInviteCode(memberId), "친구추가 링크 재발급 성공"));
    }

    @Operation(summary = "친구추가 링크 미리보기", description = "초대 코드의 주인이 누구인지 확인합니다. 비로그인 상태에서도 호출할 수 있습니다.")
    @GetMapping("/invite/{code}")
    public ResponseEntity<ResponseDto<FriendInvitePreviewResponseDto>> getInvitePreview(@PathVariable String code) {
        return ResponseEntity.ok(ResponseDto.of(friendInviteService.getInvitePreview(code), "친구추가 링크 조회 성공"));
    }

    @Operation(summary = "친구추가 링크 수락", description = "링크 주인과 즉시 친구가 됩니다. 별도 수락 절차가 없습니다.")
    @SecurityRequirement(name = "Auth")
    @PostMapping("/invite/{code}/accept")
    public ResponseEntity<ResponseDto<FriendResponseDto>> acceptInvite(@PathVariable String code, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendInviteService.acceptInvite(memberId, code), "친구추가 성공"));
    }

    @Operation(summary = "친구 프로필 조회", description = "친구 관계 ID를 기반으로 상대방의 상세 프로필을 조회합니다.")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/{friendId}/profile")
    public ResponseEntity<ResponseDto<MemberProfileResponseDto>> getFriendProfile(
            @PathVariable Long friendId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.getFriendProfile(memberId, friendId), "친구 프로필 조회 성공"));
    }
}
