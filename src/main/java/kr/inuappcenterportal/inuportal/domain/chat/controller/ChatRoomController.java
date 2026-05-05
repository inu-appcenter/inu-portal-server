package kr.inuappcenterportal.inuportal.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatRoomCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatRoomResponseDto;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatRoomService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "ChatRoom", description = "채팅방 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다.")
    @PostMapping
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> createChatRoom(
            @Valid @RequestBody ChatRoomCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoom = chatRoomService.createChatRoom(requestDto, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(chatRoom));
    }

    @Operation(summary = "채팅방 참여", description = "특정 채팅방에 참여합니다.")
    @PostMapping("/{roomId}/join")
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> joinChatRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoom = chatRoomService.joinChatRoom(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(chatRoom));
    }

    @Operation(summary = "채팅방 정보 및 메시지 조회", description = "채팅방의 상세 정보와 최근 메시지 목록을 조회합니다.")
    @GetMapping("/{roomId}")
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> getChatRoomDetails(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoomDetails = chatRoomService.getChatRoomMessages(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(chatRoomDetails));
    }
}
