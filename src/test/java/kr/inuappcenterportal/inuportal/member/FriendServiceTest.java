package kr.inuappcenterportal.inuportal.member;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.FriendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock
    private FriendRepository friendRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private FcmAsyncService fcmAsyncService;

    @InjectMocks
    private FriendService friendService;

    @Test
    @DisplayName("친구 요청 시 FCM 알림에 correct path (/chat/list?category=친구)가 전달되어야 한다")
    void requestFriend_sendsFcmNotificationWithCorrectPath() {
        Member requester = Member.builder().studentId("202000001").roles(List.of("ROLE_USER")).build();
        requester.updateNicknameAndFire("requester", 1L);
        ReflectionTestUtils.setField(requester, "id", 1L);

        Member receiver = Member.builder().studentId("202000002").roles(List.of("ROLE_USER")).build();
        receiver.updateNicknameAndFire("receiver", 2L);
        ReflectionTestUtils.setField(receiver, "id", 2L);

        FriendRequestDto dto = new FriendRequestDto();
        ReflectionTestUtils.setField(dto, "nickname", "receiver");

        when(memberRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(memberRepository.findByNickname("receiver")).thenReturn(Optional.of(receiver));
        when(friendRepository.existsByRequesterAndReceiver(requester, receiver)).thenReturn(false);
        when(friendRepository.existsByRequesterAndReceiver(receiver, requester)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(requester, receiver)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(receiver, requester)).thenReturn(false);
        when(friendRepository.save(any(Friend.class))).thenAnswer(invocation -> {
            Friend f = invocation.getArgument(0);
            ReflectionTestUtils.setField(f, "id", 100L);
            return f;
        });

        friendService.requestFriend(1L, dto);

        verify(fcmAsyncService).sendAsyncTrackedNotification(
                eq(List.of(2L)),
                eq("친구 요청"),
                eq("requester님이 친구 요청을 보냈습니다."),
                eq(FcmMessageType.FRIEND),
                eq(100L),
                eq("/chat/list?category=친구")
        );
    }

    @Test
    @DisplayName("친구 수락 시 FCM 알림에 correct path (/chat/list?category=친구)가 전달되어야 한다")
    void acceptFriend_sendsFcmNotificationWithCorrectPath() {
        Member requester = Member.builder().studentId("202000001").roles(List.of("ROLE_USER")).build();
        requester.updateNicknameAndFire("requester", 1L);
        ReflectionTestUtils.setField(requester, "id", 1L);

        Member receiver = Member.builder().studentId("202000002").roles(List.of("ROLE_USER")).build();
        receiver.updateNicknameAndFire("receiver", 2L);
        ReflectionTestUtils.setField(receiver, "id", 2L);

        Friend friend = Friend.builder().requester(requester).receiver(receiver).status(FriendStatus.PENDING).build();
        ReflectionTestUtils.setField(friend, "id", 100L);

        when(friendRepository.findById(100L)).thenReturn(Optional.of(friend));

        friendService.acceptFriend(2L, 100L);

        verify(fcmAsyncService).sendAsyncTrackedNotification(
                eq(List.of(1L)),
                eq("친구 수락"),
                eq("receiver님이 친구 요청을 수락했습니다."),
                eq(FcmMessageType.FRIEND),
                eq(100L),
                eq("/chat/list?category=친구")
        );
    }

    @Test
    @DisplayName("친구 거절 시 FCM 알림에 correct path (/chat/list?category=친구)가 전달되어야 한다")
    void deleteFriend_reject_sendsFcmNotificationWithCorrectPath() {
        Member requester = Member.builder().studentId("202000001").roles(List.of("ROLE_USER")).build();
        requester.updateNicknameAndFire("requester", 1L);
        ReflectionTestUtils.setField(requester, "id", 1L);

        Member receiver = Member.builder().studentId("202000002").roles(List.of("ROLE_USER")).build();
        receiver.updateNicknameAndFire("receiver", 2L);
        ReflectionTestUtils.setField(receiver, "id", 2L);

        Friend friend = Friend.builder().requester(requester).receiver(receiver).status(FriendStatus.PENDING).build();
        ReflectionTestUtils.setField(friend, "id", 100L);

        when(friendRepository.findById(100L)).thenReturn(Optional.of(friend));

        friendService.deleteFriend(2L, 100L);

        verify(fcmAsyncService).sendAsyncTrackedNotification(
                eq(List.of(1L)),
                eq("친구 요청 결과"),
                eq("친구 요청이 거절되었습니다."),
                eq(FcmMessageType.FRIEND),
                eq(100L),
                eq("/chat/list?category=친구")
        );
        verify(friendRepository).delete(friend);
    }
}
