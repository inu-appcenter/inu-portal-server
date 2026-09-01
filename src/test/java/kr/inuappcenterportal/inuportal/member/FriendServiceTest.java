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
                eq("receiver님이 친구 요청을 거절했습니다."),
                eq(FcmMessageType.FRIEND),
                eq(100L),
                eq("/chat/list?category=친구")
        );
        verify(friendRepository).delete(friend);
    }

    @Test
    @DisplayName("주변 친구 조회 실패 - 필수 파라미터 누락")
    void getNearbyFriends_missingParameters_throwsException() {
        kr.inuappcenterportal.inuportal.global.exception.ex.MyException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        kr.inuappcenterportal.inuportal.global.exception.ex.MyException.class,
                        () -> friendService.getNearbyFriends(1L, null, 126.6321, 200)
                );
        org.junit.jupiter.api.Assertions.assertEquals(
                kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode.REQUIRED_LOCATION_PARAMETER,
                ex.getErrorCode()
        );
    }

    @Test
    @DisplayName("주변 친구 조회 실패 - 위경도 범위 오류")
    void getNearbyFriends_invalidRange_throwsException() {
        kr.inuappcenterportal.inuportal.global.exception.ex.MyException ex =
                org.junit.jupiter.api.Assertions.assertThrows(
                        kr.inuappcenterportal.inuportal.global.exception.ex.MyException.class,
                        () -> friendService.getNearbyFriends(1L, 37.4638, 200.0, 200)
                );
        org.junit.jupiter.api.Assertions.assertEquals(
                kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode.INVALID_LOCATION_VALUE,
                ex.getErrorCode()
        );
    }

    @Test
    @DisplayName("주변 친구 조회 성공 - 반경 필터링, 친구/차단 제외, 거리순 정렬 검증")
    void getNearbyFriends_filtersAndSortsCorrectly() {
        // Me at (37.4638, 126.6321)
        Member me = Member.builder().studentId("202000001").roles(List.of("ROLE_USER")).build();
        ReflectionTestUtils.setField(me, "id", 1L);

        // Candidate 1: ~50m away (lat: 37.4642, lon: 126.6321) -> should be included
        Member closeUser = Member.builder().studentId("202000002").roles(List.of("ROLE_USER")).build();
        closeUser.updateNicknameAndFire("closeUser", 2L);
        closeUser.updateLocation(37.4642, 126.6321);
        ReflectionTestUtils.setField(closeUser, "id", 2L);

        // Candidate 2: ~100m away (lat: 37.4647, lon: 126.6321) -> should be included after candidate 1
        Member midUser = Member.builder().studentId("202000003").roles(List.of("ROLE_USER")).build();
        midUser.updateNicknameAndFire("midUser", 3L);
        midUser.updateLocation(37.4647, 126.6321);
        ReflectionTestUtils.setField(midUser, "id", 3L);

        // Candidate 3: ~500m away -> outside 200m radius -> should be excluded
        Member farUser = Member.builder().studentId("202000004").roles(List.of("ROLE_USER")).build();
        farUser.updateNicknameAndFire("farUser", 4L);
        farUser.updateLocation(37.4683, 126.6321);
        ReflectionTestUtils.setField(farUser, "id", 4L);

        // Candidate 4: ~60m away, but already friend -> should be excluded
        Member friendUser = Member.builder().studentId("202000005").roles(List.of("ROLE_USER")).build();
        friendUser.updateNicknameAndFire("friendUser", 5L);
        friendUser.updateLocation(37.4643, 126.6321);
        ReflectionTestUtils.setField(friendUser, "id", 5L);

        // Candidate 5: ~70m away, but blocked -> should be excluded
        Member blockedUser = Member.builder().studentId("202000006").roles(List.of("ROLE_USER")).build();
        blockedUser.updateNicknameAndFire("blockedUser", 6L);
        blockedUser.updateLocation(37.4644, 126.6321);
        ReflectionTestUtils.setField(blockedUser, "id", 6L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(me));
        when(memberRepository.findActiveNearbyCandidates(any(), eq(1L)))
                .thenReturn(List.of(midUser, closeUser, farUser, friendUser, blockedUser));

        when(friendRepository.existsFriendship(1L, 2L, FriendStatus.ACCEPTED)).thenReturn(false);
        when(friendRepository.existsFriendship(1L, 3L, FriendStatus.ACCEPTED)).thenReturn(false);
        when(friendRepository.existsFriendship(1L, 4L, FriendStatus.ACCEPTED)).thenReturn(false);
        when(friendRepository.existsFriendship(1L, 5L, FriendStatus.ACCEPTED)).thenReturn(true);
        when(friendRepository.existsFriendship(1L, 6L, FriendStatus.ACCEPTED)).thenReturn(false);

        when(blockRepository.existsByBlockerAndBlocked(me, closeUser)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(closeUser, me)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(me, midUser)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(midUser, me)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(me, farUser)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(farUser, me)).thenReturn(false);
        when(blockRepository.existsByBlockerAndBlocked(me, blockedUser)).thenReturn(true);

        List<kr.inuappcenterportal.inuportal.domain.member.dto.NearbyFriendResponseDto> result =
                friendService.getNearbyFriends(1L, 37.4638, 126.6321, 200);

        org.junit.jupiter.api.Assertions.assertEquals(2, result.size());
        org.junit.jupiter.api.Assertions.assertEquals(2L, result.get(0).memberId());
        org.junit.jupiter.api.Assertions.assertEquals("closeUser", result.get(0).nickname());
        org.junit.jupiter.api.Assertions.assertEquals(3L, result.get(1).memberId());
        org.junit.jupiter.api.Assertions.assertEquals("midUser", result.get(1).nickname());
        org.junit.jupiter.api.Assertions.assertTrue(result.get(0).distanceMeters() <= result.get(1).distanceMeters());
    }
}
