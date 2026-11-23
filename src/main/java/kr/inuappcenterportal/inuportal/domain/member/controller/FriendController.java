package kr.inuappcenterportal.inuportal.domain.member.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendInviteCodeResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendInvitePreviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendAliasRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberProfileResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.NearbyFriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.service.FriendInviteService;
import kr.inuappcenterportal.inuportal.domain.member.service.FriendService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
public class FriendController implements FriendApiSpecification {

    private final FriendService friendService;
    private final FriendInviteService friendInviteService;

    @PostMapping("/request")
    public ResponseEntity<ResponseDto<Void>> requestFriend(@Valid @RequestBody FriendRequestDto requestDto, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        friendService.requestFriend(memberId, requestDto);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @GetMapping("/pending")
    public ResponseEntity<ResponseDto<List<FriendResponseDto>>> getPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.getPendingRequests(memberId)));
    }

    @GetMapping("/pending/sent")
    public ResponseEntity<ResponseDto<List<FriendResponseDto>>> getSentPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.getSentPendingRequests(memberId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ResponseDto<FriendResponseDto>> searchFriend(@RequestParam String nickname, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.searchMemberByNickname(memberId, nickname)));
    }

    @PostMapping("/{friendId}/accept")
    public ResponseEntity<ResponseDto<Void>> acceptFriend(@PathVariable Long friendId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        friendService.acceptFriend(memberId, friendId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<ResponseDto<Void>> deleteFriend(@PathVariable Long friendId, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        friendService.deleteFriend(memberId, friendId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @GetMapping
    public ResponseEntity<ResponseDto<List<FriendResponseDto>>> getFriendList(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.getFriendList(memberId)));
    }

    @PatchMapping("/{friendId}/alias")
    public ResponseEntity<ResponseDto<Void>> updateFriendAlias(
            @PathVariable Long friendId,
            @Valid @RequestBody FriendAliasRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        friendService.updateFriendAlias(memberId, friendId, requestDto);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @GetMapping("/invite-code")
    public ResponseEntity<ResponseDto<FriendInviteCodeResponseDto>> getInviteCode(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendInviteService.getOrCreateInviteCode(memberId), "친구추가 링크 조회 성공"));
    }

    @PostMapping("/invite-code/refresh")
    public ResponseEntity<ResponseDto<FriendInviteCodeResponseDto>> refreshInviteCode(@AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendInviteService.refreshInviteCode(memberId), "친구추가 링크 재발급 성공"));
    }

    @GetMapping("/invite/{code}")
    public ResponseEntity<ResponseDto<FriendInvitePreviewResponseDto>> getInvitePreview(@PathVariable String code) {
        return ResponseEntity.ok(ResponseDto.of(friendInviteService.getInvitePreview(code), "친구추가 링크 조회 성공"));
    }

    @PostMapping("/invite/{code}/accept")
    public ResponseEntity<ResponseDto<FriendResponseDto>> acceptInvite(@PathVariable String code, @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendInviteService.acceptInvite(memberId, code), "친구추가 성공"));
    }

    @GetMapping("/{friendId}/profile")
    public ResponseEntity<ResponseDto<MemberProfileResponseDto>> getFriendProfile(
            @PathVariable Long friendId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ResponseDto.of(friendService.getFriendProfile(memberId, friendId), "친구 프로필 조회 성공"));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ResponseDto<List<NearbyFriendResponseDto>>> getNearbyFriends(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "200") Integer radiusMeters,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        List<NearbyFriendResponseDto> nearbyFriends = friendService.getNearbyFriends(memberId, latitude, longitude, radiusMeters);
        return ResponseEntity.ok(ResponseDto.of(nearbyFriends, "주변 친구 후보 조회 성공"));
    }
}
