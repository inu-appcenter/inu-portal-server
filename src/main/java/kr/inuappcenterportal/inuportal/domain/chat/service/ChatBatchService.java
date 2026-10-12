package kr.inuappcenterportal.inuportal.domain.chat.service;

import kr.inuappcenterportal.inuportal.domain.chat.domain.ChatMessage;
import kr.inuappcenterportal.inuportal.domain.chat.enums.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Service
public class ChatBatchService {

    private static final String INSERT_SQL = "INSERT INTO chat_message (id, chat_room_id, member_id, content, senderNickname, imageCount, messageType, extraData, create_date, modified_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    // chat_message.extraData 컬럼 길이
    private static final int MAX_EXTRA_DATA_LENGTH = 2000;

    private final JdbcTemplate jdbcTemplate;
    private final BlockingQueue<ChatMessage> messageQueue = new LinkedBlockingQueue<>();

    public ChatBatchService(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

        try {
            jdbcTemplate.batchUpdate(INSERT_SQL, messagesToSave, messagesToSave.size(), ChatBatchService::bind);
        } catch (DataAccessException e) {
            // 한 건 때문에 묶음 전체가 실패하면 나머지 메시지까지 사라진다. 한 건씩 다시 저장해 문제 있는 메시지만 걸러낸다.
            log.error("채팅 메시지 묶음 저장 실패, 한 건씩 다시 저장합니다. size:{}", messagesToSave.size(), e);
            saveOneByOne(messagesToSave);
        }
    }

    private void saveOneByOne(List<ChatMessage> messages) {
        for (ChatMessage message : messages) {
            try {
                jdbcTemplate.update(INSERT_SQL, ps -> bind(ps, message));
            } catch (DuplicateKeyException e) {
                // 묶음 저장이 일부만 반영된 뒤 실패한 경우. 이미 저장된 메시지다.
                log.debug("이미 저장된 채팅 메시지 messageId:{}", message.getId());
            } catch (DataAccessException e) {
                log.error("채팅 메시지 저장 실패 messageId:{}, roomId:{}, messageType:{}",
                        message.getId(), message.getChatRoom().getId(), message.getMessageType(), e);
            }
        }
    }

    private static void bind(PreparedStatement ps, ChatMessage message) throws SQLException {
        ps.setLong(1, message.getId());
        ps.setLong(2, message.getChatRoom().getId());
        ps.setLong(3, message.getSender().getId());
        ps.setString(4, message.getContent());
        ps.setString(5, message.getSenderNickname());
        ps.setInt(6, message.getImageCount());
        // 부가 데이터가 컬럼보다 길면 저장이 실패해 메시지가 통째로 사라진다.
        // 잘린 JSON은 쓸 수 없으니 부가 데이터 없이 일반 메시지로 저장한다(부가 데이터를 저장하지 않던 예전과 같은 결과).
        boolean extraDataTooLong = message.getExtraData() != null
                && message.getExtraData().length() > MAX_EXTRA_DATA_LENGTH;
        ps.setString(7, extraDataTooLong ? MessageType.TEXT.name() : message.getMessageType().name());
        ps.setString(8, extraDataTooLong ? null : message.getExtraData());
        ps.setTimestamp(9, Timestamp.valueOf(message.getCreateDate()));
        ps.setTimestamp(10, Timestamp.valueOf(message.getModifiedDate()));
    }
}
