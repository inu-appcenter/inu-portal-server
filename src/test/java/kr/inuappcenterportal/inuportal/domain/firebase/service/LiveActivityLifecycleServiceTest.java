package kr.inuappcenterportal.inuportal.domain.firebase.service;

import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.LiveActivityInstanceRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.model.LiveActivityInstance;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.LiveActivityInstanceRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveActivityLifecycleServiceTest {

    private static final long START = 1_800_000_000_000L;
    private static final long END = START + 75 * 60_000L;

    @Mock
    private LiveActivityInstanceRepository repository;

    @InjectMocks
    private LiveActivityLifecycleService service;

    private static LiveActivityInstanceRequestDto request(long start, long end) {
        return LiveActivityInstanceRequestDto.builder()
                .token("fcm")
                .activityId("A1")
                .pushToken("activity-token")
                .props("{\"phase\":\"UPCOMING\",\"courseTitle\":\"자료구조\",\"startTimestamp\":" + start
                        + ",\"endTimestamp\":" + end + "}")
                .build();
    }

    @Test
    @DisplayName("새 Activity는 props의 수업 시각과 함께 저장한다")
    void savesNewActivity() {
        when(repository.findByActivityId("A1")).thenReturn(Optional.empty());

        service.register(request(START, END), 7L);

        ArgumentCaptor<LiveActivityInstance> captor = ArgumentCaptor.forClass(LiveActivityInstance.class);
        verify(repository).save(captor.capture());
        LiveActivityInstance saved = captor.getValue();
        assertThat(saved.getMemberId()).isEqualTo(7L);
        assertThat(saved.getPushToken()).isEqualTo("activity-token");
        assertThat(saved.getStartAt()).isEqualTo(START);
        assertThat(saved.getEndAt()).isEqualTo(END);
        assertThat(saved.isOngoingSent()).isFalse();
        assertThat(saved.isEnded()).isFalse();
    }

    @Test
    @DisplayName("같은 수업이면 토큰만 갱신하고 진행 상태는 유지한다")
    void keepsProgressForSameClass() {
        LiveActivityInstance existing = LiveActivityInstance.builder().memberId(7L).activityId("A1").fcmToken("fcm")
                .pushToken("old").propsJson("{}").startAt(START).endAt(END).build();
        existing.markOngoingSent();
        when(repository.findByActivityId("A1")).thenReturn(Optional.of(existing));

        service.register(request(START, END), 7L);

        assertThat(existing.getPushToken()).isEqualTo("activity-token");
        assertThat(existing.isOngoingSent()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("앱이 같은 Activity를 다음 수업으로 바꿔 쓰면 진행 상태를 처음부터 다시 잡는다")
    void resetsProgressWhenClassChanges() {
        LiveActivityInstance existing = LiveActivityInstance.builder().memberId(7L).activityId("A1").fcmToken("fcm")
                .pushToken("t").propsJson("{}").startAt(START).endAt(END).build();
        existing.markOngoingSent();
        existing.markEnded();
        when(repository.findByActivityId("A1")).thenReturn(Optional.of(existing));

        service.register(request(END + 30 * 60_000L, END + 105 * 60_000L), 7L);

        assertThat(existing.isOngoingSent()).isFalse();
        assertThat(existing.isEnded()).isFalse();
        assertThat(existing.getStartAt()).isEqualTo(END + 30 * 60_000L);
    }

    @Test
    @DisplayName("수업 시각이 없거나 잘못된 props는 거부한다")
    void rejectsInvalidProps() {
        assertThatThrownBy(() -> service.register(LiveActivityInstanceRequestDto.builder()
                .token("fcm").activityId("A1").pushToken("t").props("{\"phase\":\"UPCOMING\"}").build(), 7L))
                .isInstanceOf(MyException.class);
        assertThatThrownBy(() -> service.register(request(END, START), 7L)).isInstanceOf(MyException.class);
        assertThatThrownBy(() -> service.register(LiveActivityInstanceRequestDto.builder()
                .token("fcm").activityId("A1").pushToken("t").props("not json").build(), 7L))
                .isInstanceOf(MyException.class);
    }

    @Test
    @DisplayName("withPhase는 phase만 바꾸고 나머지 props는 그대로 둔다")
    void withPhaseKeepsOtherProps() {
        String result = LiveActivityLifecycleService.withPhase(
                "{\"phase\":\"UPCOMING\",\"courseTitle\":\"자료구조\",\"startTimestamp\":1}", "ONGOING");

        assertThat(result).isEqualTo("{\"phase\":\"ONGOING\",\"courseTitle\":\"자료구조\",\"startTimestamp\":1}");
    }
}
