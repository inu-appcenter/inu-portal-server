package kr.inuappcenterportal.inuportal.domain.chat.domain;

import io.hypersistence.tsid.TSID;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ChatMessageTest {

    @Test
    @DisplayName("ChatMessage 빌더 테스트 - TSID 및 시간 직접 주입")
    void chatMessageBuilderTest() {
        // given
        Long tsid = TSID.fast().toLong();
        LocalDateTime now = LocalDateTime.now();
        ChatRoom chatRoom = ChatRoom.builder()
                .title("테스트 방")
                .maxCapacity(10)
                .isAnonymous(true)
                .build();
        Member sender = Member.builder()
                .studentId("202401234")
                .roles(java.util.List.of("ROLE_USER"))
                .build();

        // when
        ChatMessage chatMessage = ChatMessage.builder()
                .id(tsid)
                .chatRoom(chatRoom)
                .sender(sender)
                .content("안녕하세요")
                .senderNickname("익명1")
                .imageCount(0)
                .createDate(now)
                .modifiedDate(now)
                .build();

        // then
        assertThat(chatMessage.getId()).isEqualTo(tsid);
        assertThat(chatMessage.getContent()).isEqualTo("안녕하세요");
        assertThat(chatMessage.getCreateDate()).isEqualTo(now);
        assertThat(chatMessage.getModifiedDate()).isEqualTo(now);
    }
}
