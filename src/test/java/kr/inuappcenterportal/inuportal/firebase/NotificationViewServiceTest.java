package kr.inuappcenterportal.inuportal.firebase;

import kr.inuappcenterportal.inuportal.domain.firebase.contorller.FcmController;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.MemberFcmMessageRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.firebase.service.NotificationViewService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationViewServiceTest {

    @Mock
    MemberFcmMessageRepository memberFcmMessageRepository;

    @InjectMocks
    NotificationViewService notificationViewService;

    @Test
    @DisplayName("같은 회원의 방문 기록이 진행 중이면 겹쳐 들어온 요청은 건너뛴다")
    void skipsWhileSameMemberInFlight() {
        // 첫 작업이 UPDATE를 수행하는 도중 같은 회원의 요청이 하나 더 들어온 상황
        doAnswer(invocation -> {
            notificationViewService.recordVisit(1L);
            return 0;
        }).when(memberFcmMessageRepository).markAsReadByViewCount(eq(1L), anyInt(), any());

        notificationViewService.recordVisit(1L);

        verify(memberFcmMessageRepository, times(1)).markAsReadByViewCount(eq(1L), anyInt(), any());
        verify(memberFcmMessageRepository, times(1)).incrementViewCountForAllUnread(1L);
    }

    @Test
    @DisplayName("작업이 끝나면 같은 회원의 다음 방문은 다시 기록된다")
    void releasesAfterCompletion() {
        notificationViewService.recordVisit(1L);
        notificationViewService.recordVisit(1L);

        verify(memberFcmMessageRepository, times(2)).incrementViewCountForAllUnread(1L);
    }

    @Test
    @DisplayName("UPDATE가 실패해도 예외를 던지지 않고, 다음 방문은 막히지 않는다")
    void swallowsFailureAndReleases() {
        when(memberFcmMessageRepository.markAsReadByViewCount(eq(1L), anyInt(), any()))
                .thenThrow(new RuntimeException("lock timeout"))
                .thenReturn(0);

        notificationViewService.recordVisit(1L);
        notificationViewService.recordVisit(1L);

        verify(memberFcmMessageRepository, times(1)).incrementViewCountForAllUnread(1L);
    }

    @Test
    @DisplayName("회원 id가 없으면 아무것도 하지 않는다")
    void ignoresNullMember() {
        notificationViewService.recordVisit(null);

        verifyNoInteractions(memberFcmMessageRepository);
    }

    @Test
    @DisplayName("알림함은 1페이지 조회일 때만 방문으로 기록한다")
    void controllerRecordsVisitOnlyOnFirstPage() {
        FcmService fcmService = mock(FcmService.class);
        NotificationViewService viewService = mock(NotificationViewService.class);
        FcmController controller = new FcmController(fcmService, null, null, null, viewService);
        Member member = Member.builder()
                .studentId("202000005")
                .roles(Collections.singletonList("ROLE_USER"))
                .build();
        ReflectionTestUtils.setField(member, "id", 10L);

        controller.checkNotification(member, 2);
        controller.checkNotification(null, 1);
        verify(viewService, never()).recordVisit(any());

        controller.checkNotification(member, 1);
        verify(viewService, times(1)).recordVisit(10L);
    }
}
