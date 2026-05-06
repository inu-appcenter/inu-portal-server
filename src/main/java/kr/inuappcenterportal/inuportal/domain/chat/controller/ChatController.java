package kr.inuappcenterportal.inuportal.domain.chat.controller;

import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageDto;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatRoom;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageRequestDto;
import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageResponseDto;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomRepository;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatBatchService;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatRedisService;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatRoomService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatNotificationService chatNotificationService;

    @MessageMapping("/chat/sendMessage")
    public void sendMessage(ChatMessageDto chatMessage) {
        // 1. 메시지를 해당 채팅방 구독자에게 즉시 전송
        messagingTemplate.convertAndSend("/topic/chat/room/" + chatMessage.getRoomId(), chatMessage);

        // 2. 푸시 알림 처리를 위해 서비스로 메시지 전달
        chatNotificationService.processChatMessage(chatMessage);
    private final ChatRedisService chatRedisService;
    private final ChatBatchService chatBatchService;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final ObjectMapper objectMapper;

    @MessageMapping("/message")
    public void message(ChatMessageRequestDto messageDto, Authentication authentication) {
        Long memberId = Long.parseLong(authentication.getName());
        chatRoomService.sendMessage(messageDto, memberId);
    }
}
