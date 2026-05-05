package kr.inuappcenterportal.inuportal.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageResponseDto;
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
import java.util.List;

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

    @Operation(summary = "채팅방 초기 메시지 로드", description = "채팅방 입장 시 최근 메시지 목록을 로드합니다. (Redis 캐시 우선)")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ResponseDto<List<ChatMessageResponseDto>>> getChatRoomMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        List<ChatMessageResponseDto> messages = chatRoomService.getChatRoomMessages(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(messages));
    }
}
