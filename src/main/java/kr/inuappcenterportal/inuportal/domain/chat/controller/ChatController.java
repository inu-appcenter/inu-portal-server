package kr.inuappcenterportal.inuportal.domain.chat.controller;

import kr.inuappcenterportal.inuportal.domain.chat.dto.ChatMessageDto;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

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
    }
}
