package kr.inuappcenterportal.inuportal.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatRoomService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import kr.inuappcenterportal.inuportal.domain.chat.dto.*;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberProfileResponseDto;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@Tag(name = "ChatRoom", description = "채팅방 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "전체 오픈채팅방 목록 조회", description = "활성화된 모든 오픈채팅방 목록을 페이징하여 조회합니다. 로그인한 경우 참여 여부가 포함됩니다.")
    @GetMapping("/open")
    public ResponseEntity<ResponseDto<Page<OpenChatRoomResponseDto>>> getOpenChatRooms(
            @Parameter(description = "검색어 (제목)") @RequestParam(required = false) String search,
            @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = (userDetails != null) ? Long.parseLong(userDetails.getUsername()) : null;
        Pageable pageable = PageRequest.of(page, size);
        Page<OpenChatRoomResponseDto> openChatRooms = chatRoomService.getOpenChatRooms(memberId, search, pageable);
        return ResponseEntity.ok(ResponseDto.of(openChatRooms));
    }

    @Operation(summary = "내가 참여중인 채팅방 목록 조회")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/my")
    public ResponseEntity<ResponseDto<List<MyChatRoomResponseDto>>> getMyChatRooms(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        List<MyChatRoomResponseDto> myChatRooms = chatRoomService.getMyChatRooms(memberId);
        return ResponseEntity.ok(ResponseDto.of(myChatRooms));
    }

    @Operation(summary = "전체 안 읽은 메시지 수 조회", description = "로그인한 유저가 참여 중인 모든 채팅방의 안 읽은 메시지 수 합계를 조회합니다.")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/unread-total-count")
    public ResponseEntity<ResponseDto<UnreadTotalCountResponseDto>> getTotalUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        UnreadTotalCountResponseDto countDto = chatRoomService.getTotalUnreadCount(memberId);
        return ResponseEntity.ok(ResponseDto.of(countDto));
    }

    @Operation(summary = "개인/그룹 채팅방 생성/조회", description = "상대방 유저 ID 리스트를 기반으로 개인톡/그룹톡방을 생성하거나 기존 방을 반환합니다. 모든 상대방과 친구여야 합니다.")
    @SecurityRequirement(name = "Auth")
    @PostMapping("/personal")
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> getOrCreatePersonalChatRoom(
            @Valid @RequestBody PersonalChatRoomRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoom = chatRoomService.getOrCreatePersonalChatRoom(requestDto, memberId);
        return ResponseEntity.ok(ResponseDto.of(chatRoom));
    }

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다. 생성자는 자동으로 채팅방에 참여됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공", content = @Content(schema = @Schema(implementation = ChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @SecurityRequirement(name = "Auth")
    @PostMapping
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> createChatRoom(
            @Parameter(description = "채팅방 생성 정보", required = true) @Valid @RequestBody ChatRoomCreateRequestDto requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoom = chatRoomService.createChatRoom(requestDto, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(chatRoom));
    }

    @Operation(summary = "채팅방 참여", description = "기존 채팅방에 참여합니다. 이미 참여 중인 경우에도 성공 응답을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "채팅방 참여 성공", content = @Content(schema = @Schema(implementation = ChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방"),
            @ApiResponse(responseCode = "409", description = "채팅방 인원이 가득 참 (CHATROOM_FULL)")
    })
    @SecurityRequirement(name = "Auth")
    @PostMapping("/{roomId}/join")
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> joinChatRoom(
            @Parameter(description = "참여할 채팅방의 ID", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoom = chatRoomService.joinChatRoom(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(chatRoom));
    }

    @Operation(summary = "채팅방 나가기", description = "채팅방을 나갑니다. 실제 데이터는 삭제되지 않으며 상태만 변경됩니다.")
    @SecurityRequirement(name = "Auth")
    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<ResponseDto<Void>> leaveChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        chatRoomService.leaveChatRoom(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "채팅방 폐쇄", description = "오픈채팅방을 폐쇄합니다. 신규 참여가 불가능해집니다.")
    @SecurityRequirement(name = "Auth")
    @PatchMapping("/{roomId}/close")
    public ResponseEntity<ResponseDto<Void>> closeChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        chatRoomService.closeChatRoom(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "채팅방 이름 변경", description = "채팅방의 이름을 변경합니다.")
    @SecurityRequirement(name = "Auth")
    @PatchMapping("/{roomId}/title")
    public ResponseEntity<ResponseDto<Void>> updateChatRoomTitle(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomTitleUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        chatRoomService.updateChatRoomTitle(roomId, requestDto, memberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "채팅방 정보 수정", description = "채팅방의 이름, 설명, 썸네일, 최대 인원을 수정합니다. 방장만 가능합니다.")
    @SecurityRequirement(name = "Auth")
    @PatchMapping("/{roomId}/info")
    public ResponseEntity<ResponseDto<Void>> updateChatRoomInfo(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        chatRoomService.updateRoomInfo(roomId, requestDto, memberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "방장 위임", description = "채팅방의 방장을 다른 멤버에게 위임합니다. 방장만 가능합니다.")
    @SecurityRequirement(name = "Auth")
    @PatchMapping("/{roomId}/delegate")
    public ResponseEntity<ResponseDto<Void>> delegateOwner(
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomDelegateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        chatRoomService.delegateOwner(roomId, requestDto, memberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "멤버 강퇴", description = "채팅방의 특정 멤버를 강퇴합니다. 방장만 가능하며, 강퇴된 사용자는 재입장이 불가합니다.")
    @SecurityRequirement(name = "Auth")
    @DeleteMapping("/{roomId}/members/{targetChatRoomMemberId}")
    public ResponseEntity<ResponseDto<Void>> kickMember(
            @PathVariable Long roomId,
            @PathVariable Long targetChatRoomMemberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        chatRoomService.kickMember(roomId, targetChatRoomMemberId, memberId);
        return ResponseEntity.ok(ResponseDto.of(null));
    }

    @Operation(summary = "채팅방 참여자 목록 조회", description = "채팅방에 참여 중인 유저 목록을 조회합니다.")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/{roomId}/members")
    public ResponseEntity<ResponseDto<List<ChatRoomMemberResponseDto>>> getParticipants(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        List<ChatRoomMemberResponseDto> members = chatRoomService.getParticipants(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(members));
    }

    @Operation(summary = "채팅방 내 유저 프로필 조회", description = "채팅방 내 특정 참가자의 프로필을 조회합니다. 익명방인 경우 학번/횃불이 정보가 숨겨지며 임시 닉네임만 표시됩니다.")
    @SecurityRequirement(name = "Auth")
    @GetMapping("/{roomId}/members/{chatRoomMemberId}/profile")
    public ResponseEntity<ResponseDto<MemberProfileResponseDto>> getChatRoomMemberProfile(
            @PathVariable Long roomId,
            @PathVariable Long chatRoomMemberId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        MemberProfileResponseDto profile = chatRoomService.getChatRoomMemberProfile(roomId, chatRoomMemberId, memberId);
        return ResponseEntity.ok(ResponseDto.of(profile, "참여자 프로필 조회 성공"));
    }

    @Operation(summary = "채팅방 정보 및 메시지 조회", description = "특정 채팅방의 상세 정보(참여 인원, 내 해시 등)와 최근 메시지 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "채팅방에 참여하지 않은 사용자 (NOT_CHATROOM_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방")
    })
    @SecurityRequirement(name = "Auth")
    @GetMapping("/{roomId}")
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> getChatRoomDetails(
            @Parameter(description = "조회할 채팅방의 ID", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoomDetails = chatRoomService.getChatRoomMessages(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(chatRoomDetails));
    }

    @Operation(summary = "이전 메시지 조회", description = "특정 ID보다 이전에 작성된 메시지 50개를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ChatMessageResponseDto.class)))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "채팅방에 참여하지 않은 사용자 (NOT_CHATROOM_MEMBER)")
    })
    @SecurityRequirement(name = "Auth")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ResponseDto<List<ChatMessageResponseDto>>> getOlderMessages(
            @Parameter(description = "채팅방의 ID", required = true) @PathVariable Long roomId,
            @Parameter(description = "가장 오래된 메시지의 ID (이 ID 이전의 메시지를 조회)", required = true) @RequestParam Long lastId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        List<ChatMessageResponseDto> messages = chatRoomService.getOlderMessages(roomId, memberId, lastId);
        return ResponseEntity.ok(ResponseDto.of(messages));
    }

    @Operation(summary = "채팅방 최신 메시지 2개 조회 (Public)", description = "로그인 없이 누구나 특정 오픈 채팅방의 최신 메시지 2개를 조회할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(array = @ArraySchema(schema = @Schema(implementation = PublicChatMessageResponseDto.class)))),
            @ApiResponse(responseCode = "403", description = "오픈채팅방이 아님"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방")
    })
    @GetMapping("/{roomId}/messages/public")
    public ResponseEntity<ResponseDto<List<PublicChatMessageResponseDto>>> getPublicMessages(
            @PathVariable Long roomId) {
        List<PublicChatMessageResponseDto> messages = chatRoomService.getPublicMessages(roomId);
        return ResponseEntity.ok(ResponseDto.of(messages));
    }

    @Operation(summary = "채팅방 푸시 알림 설정 토글", description = "특정 채팅방의 푸시 알림을 켜거나 끕니다.")
    @SecurityRequirement(name = "Auth")
    @PatchMapping("/{roomId}/push-setting")
    public ResponseEntity<ResponseDto<Boolean>> toggleRoomPush(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        boolean isEnabled = chatRoomService.toggleRoomPush(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(isEnabled, "채팅방 알림 설정이 " + (isEnabled ? "켜졌습니다" : "꺼졌습니다")));
    }
}
