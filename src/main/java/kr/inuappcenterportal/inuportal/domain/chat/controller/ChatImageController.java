package kr.inuappcenterportal.inuportal.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageRequestDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageResponseDto;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatRoomService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Tag(name="Chat Images", description = "채팅 이미지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatImageController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "이미지 포함 채팅 메시지 전송")
    @PostMapping(value = "/messages", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseDto<ChatMessageResponseDto>> sendMessageWithImages(
            @RequestPart ChatMessageRequestDto messageDto,
            @RequestPart(required = false) List<MultipartFile> images,
            Authentication authentication) throws IOException {
        
        Long memberId = Long.parseLong(authentication.getName());
        if (images == null || images.isEmpty()) {
            chatRoomService.sendMessage(messageDto, memberId);
            return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(null, "메시지 전송 성공 (이미지 없음)"));
        }
        
        ChatMessageResponseDto response = chatRoomService.sendMessageWithImages(messageDto, images, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseDto.of(response, "메시지 전송 성공"));
    }
}
