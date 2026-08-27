package kr.inuappcenterportal.inuportal.firebase;

import com.google.firebase.messaging.FirebaseMessaging;
import kr.inuappcenterportal.inuportal.config.FcmTestAsyncConfig;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.model.MemberFcmMessage;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncExecutor;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmTransactionService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.metric.FcmMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {FcmTestAsyncConfig.class, FcmService.class, FcmTransactionService.class})
class NotificationReadByFcmMessageIdTest {

    @MockBean
    private FcmTokenRepository fcmTokenRepository;

    @MockBean
    private FcmMessageRepository fcmMessageRepository;

    @MockBean
    private MemberFcmMessageRepository memberFcmMessageRepository;

    @MockBean
    private FirebaseMessaging firebaseMessaging;

    @MockBean
    private MemberRepository memberRepository;

    @MockBean
    private SemesterRepository semesterRepository;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private FcmTransactionService fcmTransactionService;

    @MockBean
    private FcmAsyncExecutor fcmAsyncExecutor;

    @MockBean
    private FcmMetrics fcmMetrics;

    @Autowired
    private FcmService fcmService;

    @Test
    @DisplayName("푸시 payload의 fcmMessageId로 해당 회원의 알림함 행이 읽음 처리된다")
    void marksMemberOwnRowAsRead() {
        MemberFcmMessage message = memberFcmMessage(1001L, 55L, 69L);

        when(memberFcmMessageRepository.findAllByFcmMessageIdAndMemberId(eq(55L), eq(69L)))
                .thenReturn(List.of(message));

        fcmService.markNotificationAsReadByFcmMessageId(createMember(69L), 55L);

        assertThat(message.isRead()).isTrue();
        assertThat(message.getReadAt()).isNotNull();
    }

    @Test
    @DisplayName("같은 알림이라도 다른 회원의 행은 조회되지 않아 읽음 처리되지 않는다")
    void doesNotMarkAnotherMembersRow() {
        when(memberFcmMessageRepository.findAllByFcmMessageIdAndMemberId(eq(55L), eq(96L)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> fcmService.markNotificationAsReadByFcmMessageId(createMember(96L), 55L))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("(fcmMessageId, memberId) 중복 행이 있어도 모두 읽음 처리된다")
    void marksEveryMatchingRow() {
        MemberFcmMessage first = memberFcmMessage(1001L, 55L, 69L);
        MemberFcmMessage second = memberFcmMessage(1002L, 55L, 69L);

        when(memberFcmMessageRepository.findAllByFcmMessageIdAndMemberId(eq(55L), eq(69L)))
                .thenReturn(List.of(first, second));

        fcmService.markNotificationAsReadByFcmMessageId(createMember(69L), 55L);

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
    }

    @Test
    @DisplayName("비로그인 상태나 식별자 누락 시 조회 없이 무시한다")
    void ignoresWhenMemberOrIdMissing() {
        fcmService.markNotificationAsReadByFcmMessageId(null, 55L);
        fcmService.markNotificationAsReadByFcmMessageId(createMember(69L), null);

        verifyNoInteractions(memberFcmMessageRepository);
    }

    private MemberFcmMessage memberFcmMessage(Long id, Long fcmMessageId, Long memberId) {
        MemberFcmMessage message = MemberFcmMessage.of(fcmMessageId, memberId, FcmMessageType.POST_REPLY);
        ReflectionTestUtils.setField(message, "id", id);
        return message;
    }

    private Member createMember(Long id) {
        Member member = Member.builder()
                .studentId("201900000")
                .roles(List.of("ROLE_USER"))
                .build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}
