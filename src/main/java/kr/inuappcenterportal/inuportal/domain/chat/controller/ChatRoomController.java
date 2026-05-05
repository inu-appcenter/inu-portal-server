package kr.inuappcenterportal.inuportal.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

@Tag(name = "ChatRoom", description = "채팅방 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat-rooms")
@SecurityRequirement(name = "Auth") // 이 컨트롤러의 모든 API는 인증 필요
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "채팅방 생성", description = "새로운 채팅방을 생성합니다. 생성자는 자동으로 채팅방에 참여됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "채팅방 생성 성공", content = @Content(schema = @Schema(implementation = ChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
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
    @PostMapping("/{roomId}/join")
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> joinChatRoom(
            @Parameter(description = "참여할 채팅방의 ID", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoom = chatRoomService.joinChatRoom(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(chatRoom));
    }

    @Operation(summary = "채팅방 정보 및 메시지 조회", description = "특정 채팅방의 상세 정보(참여 인원, 내 해시 등)와 최근 메시지 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "채팅방에 참여하지 않은 사용자 (NOT_CHATROOM_MEMBER)"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방")
    })
    @GetMapping("/{roomId}")
    public ResponseEntity<ResponseDto<ChatRoomResponseDto>> getChatRoomDetails(
            @Parameter(description = "조회할 채팅방의 ID", required = true) @PathVariable Long roomId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserDetails userDetails) {
        Long memberId = Long.parseLong(userDetails.getUsername());
        ChatRoomResponseDto chatRoomDetails = chatRoomService.getChatRoomMessages(roomId, memberId);
        return ResponseEntity.ok(ResponseDto.of(chatRoomDetails));
    }
}
