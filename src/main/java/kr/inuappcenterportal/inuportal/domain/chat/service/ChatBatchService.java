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

    @Scheduled(fixedDelay = 2000) // 2초마다 실행
    public void saveMessages() {
        if (messageQueue.isEmpty()) {
            return;
        }

        List<ChatMessage> messagesToSave = new ArrayList<>();
        messageQueue.drainTo(messagesToSave);

        if (messagesToSave.isEmpty()) {
            return;
        }

        log.info("Saving {} messages to DB", messagesToSave.size());

        String sql = "INSERT INTO chat_message (chat_room_id, member_id, content, sender_nickname, image_count, create_date, modified_date) VALUES (?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql,
                messagesToSave,
                messagesToSave.size(),
                (ps, message) -> {
                    ps.setLong(1, message.getChatRoom().getId());
                    ps.setLong(2, message.getSender().getId());
                    ps.setString(3, message.getContent());
                    ps.setString(4, message.getSenderNickname());
                    ps.setInt(5, message.getImageCount());
                    ps.setTimestamp(6, Timestamp.valueOf(message.getCreateDate()));
                    ps.setTimestamp(7, Timestamp.valueOf(message.getModifiedDate()));
                });
    }
}
