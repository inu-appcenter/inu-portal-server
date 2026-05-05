package kr.inuappcenterportal.inuportal.domain.chat.service;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatBatchService {

    private final JdbcTemplate jdbcTemplate;
    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();

    public void addMessageToQueue(ChatMessage chatMessage) {
        messageQueue.add(chatMessage);
    }

    @Scheduled(fixedDelay = 2000)
    public void saveMessages() {
        if (messageQueue.isEmpty()) return;

        List<ChatMessage> messagesToSave = new ArrayList<>();
        messageQueue.drainTo(messagesToSave);

        if (messagesToSave.isEmpty()) return;

        log.info("DB에 메시지 저장 중 {} ", messagesToSave.size());

        String sql = "INSERT INTO chat_message (id, chat_room_id, member_id, content, senderNickname, imageCount, create_date, modified_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql,
                messagesToSave,
                messagesToSave.size(),
                (ps, message) -> {
                    ps.setLong(1, message.getId());
                    ps.setLong(2, message.getChatRoom().getId());
                    ps.setLong(3, message.getSender().getId());
                    ps.setString(4, message.getContent());
                    ps.setString(5, message.getSenderNickname());
                    ps.setInt(6, message.getImageCount());
                    ps.setTimestamp(7, Timestamp.valueOf(message.getCreateDate()));
                    ps.setTimestamp(8, Timestamp.valueOf(message.getModifiedDate()));
                });
    }
}
