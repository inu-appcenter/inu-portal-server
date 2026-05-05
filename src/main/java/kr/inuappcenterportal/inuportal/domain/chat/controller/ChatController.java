package kr.inuappcenterportal.inuportal.domain.chat.controller;

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
    private final ChatRedisService chatRedisService;
    private final ChatBatchService chatBatchService;
    private final MemberRepository memberRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomService chatRoomService;
    private final ObjectMapper objectMapper;

    @MessageMapping("/message")
    public void message(ChatMessageRequestDto messageDto, Authentication authentication) {
        Long memberId = Long.parseLong(authentication.getName());
        Member sender = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        String nickname;
        if (messageDto.getIsAnonymous()) {
            nickname = chatRedisService.getOrAssignAnonymousNickname(messageDto.getRoomId(), memberId);
        } else {
            nickname = sender.getNickname();
        }

        LocalDateTime now = LocalDateTime.now();

        String senderHash = chatRoomService.getSenderHash(memberId);

        // 메시지 생성 및 브로드캐스팅
        ChatMessageResponseDto responseDto = ChatMessageResponseDto.builder()
                .roomId(messageDto.getRoomId())
                .senderNickname(nickname)
                .senderHash(senderHash)
                .content(messageDto.getContent())
                .createDate(now)
                .build();

        messagingTemplate.convertAndSend("/sub/room/" + messageDto.getRoomId(), responseDto);

        // Redis에 메시지 캐싱
        try {
            String messageJson = objectMapper.writeValueAsString(responseDto);
            chatRedisService.saveMessageToCache(messageDto.getRoomId(), messageJson);
        } catch (JsonProcessingException e) {
            log.error("메시지 캐싱 중 직렬화 오류 발생: {}", responseDto, e);
        }

        // 비동기 저장을 위해 메시지 큐에 삽입
        ChatRoom chatRoom = chatRoomRepository.findById(messageDto.getRoomId())
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_CHATROOM));

        ChatMessage chatMessage = ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(messageDto.getContent())
                .senderNickname(nickname)
                .build();
        
        chatBatchService.addMessageToQueue(chatMessage);
    }
}
