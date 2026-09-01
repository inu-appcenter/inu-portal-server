package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendAliasRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberProfileResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.NearbyFriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final MemberRepository memberRepository;
    private final BlockRepository blockRepository;
    private final FcmAsyncService fcmAsyncService;

    @Transactional
    public void requestFriend(Long memberId, FriendRequestDto requestDto) {
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        Member receiver = memberRepository.findByNickname(requestDto.getNickname())
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        if (requester.getId().equals(receiver.getId())) {
            throw new MyException(MyErrorCode.NOT_SELF_FRIEND_REQUEST);
        }

        if (friendRepository.existsByRequesterAndReceiver(requester, receiver) ||
                friendRepository.existsByRequesterAndReceiver(receiver, requester)) {
            throw new MyException(MyErrorCode.ALREADY_FRIEND_OR_REQUESTED);
        }

        if (blockRepository.existsByBlockerAndBlocked(requester, receiver) ||
                blockRepository.existsByBlockerAndBlocked(receiver, requester)) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        Friend friend = Friend.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendStatus.PENDING)
                .build();
        friendRepository.save(friend);

        fcmAsyncService.sendAsyncTrackedNotification(List.of(receiver.getId()), "친구 요청", requester.getNickname() + "님이 친구 요청을 보냈습니다.", FcmMessageType.FRIEND, friend.getId(), "/chat/list?category=친구");
    }

    @Transactional(readOnly = true)
    public List<FriendResponseDto> getPendingRequests(Long memberId) {
        Member receiver = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        List<Friend> requests = friendRepository.findAllByReceiverAndStatus(receiver, FriendStatus.PENDING);

        return requests.stream().map(f -> FriendResponseDto.builder()
                .friendId(f.getId())
                .nickname(f.getRequester().getNickname())
                .studentId(f.getRequester().getMaskedStudentId())
                .fireId(f.getRequester().getFireId())
                .department(f.getRequester().getDepartment() == null ? null : f.getRequester().getDepartment().getDepartmentName())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendResponseDto> getSentPendingRequests(Long memberId) {
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        List<Friend> requests = friendRepository.findAllByRequesterAndStatus(requester, FriendStatus.PENDING);

        return requests.stream().map(f -> FriendResponseDto.builder()
                .friendId(f.getId())
                .nickname(f.getReceiver().getNickname())
                .studentId(f.getReceiver().getMaskedStudentId())
                .fireId(f.getReceiver().getFireId())
                .department(f.getReceiver().getDepartment() == null ? null : f.getReceiver().getDepartment().getDepartmentName())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FriendResponseDto searchMemberByNickname(Long memberId, String nickname) {
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        Member target = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        if (blockRepository.existsByBlockerAndBlocked(requester, target) ||
                blockRepository.existsByBlockerAndBlocked(target, requester)) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        return FriendResponseDto.builder()
                .nickname(target.getNickname())
                .studentId(target.getMaskedStudentId())
                .fireId(target.getFireId())
                .department(target.getDepartment() == null ? null : target.getDepartment().getDepartmentName())
                .build();
    }

    @Transactional
    public void acceptFriend(Long memberId, Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_FRIEND_REQUEST));

        if (!friend.getReceiver().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.HAS_NOT_FRIEND_AUTHORIZATION);
        }

        friend.accept();

        fcmAsyncService.sendAsyncTrackedNotification(List.of(friend.getRequester().getId()), "친구 수락", friend.getReceiver().getNickname() + "님이 친구 요청을 수락했습니다.", FcmMessageType.FRIEND, friend.getId(), "/chat/list?category=친구");
    }

    @Transactional
    public void deleteFriend(Long memberId, Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_FRIEND_REQUEST));

        if (!friend.getRequester().getId().equals(memberId) && !friend.getReceiver().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.HAS_NOT_FRIEND_AUTHORIZATION);
        }

        if (friend.getStatus() == FriendStatus.PENDING && friend.getReceiver().getId().equals(memberId)) {
            fcmAsyncService.sendAsyncTrackedNotification(List.of(friend.getRequester().getId()), "친구 요청 결과", friend.getReceiver().getNickname() + "님이 친구 요청을 거절했습니다.", FcmMessageType.FRIEND, friend.getId(), "/chat/list?category=친구");
        }

        friendRepository.delete(friend);
    }

    @Transactional(readOnly = true)
    public List<FriendResponseDto> getFriendList(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        List<Friend> sent = friendRepository.findAllByRequesterAndStatus(member, FriendStatus.ACCEPTED);
        List<Friend> received = friendRepository.findAllByReceiverAndStatus(member, FriendStatus.ACCEPTED);

        List<FriendResponseDto> list = sent.stream().map(f -> FriendResponseDto.builder()
                .friendId(f.getId())
                .friendMemberId(f.getReceiver().getId())
                .nickname(f.getReceiver().getNickname())
                .studentId(f.getReceiver().getMaskedStudentId())
                .fireId(f.getReceiver().getFireId())
                .friendAlias(f.getRequesterAlias())
                .department(f.getReceiver().getDepartment() == null ? null : f.getReceiver().getDepartment().getDepartmentName())
                .build()
        ).collect(Collectors.toList());

        list.addAll(received.stream().map(f -> FriendResponseDto.builder()
                .friendId(f.getId())
                .friendMemberId(f.getRequester().getId())
                .nickname(f.getRequester().getNickname())
                .studentId(f.getRequester().getMaskedStudentId())
                .fireId(f.getRequester().getFireId())
                .friendAlias(f.getReceiverAlias())
                .department(f.getRequester().getDepartment() == null ? null : f.getRequester().getDepartment().getDepartmentName())
                .build()
        ).collect(Collectors.toList()));

        return list;
    }

    @Transactional
    public void updateFriendAlias(Long memberId, Long friendId, FriendAliasRequestDto requestDto) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_FRIEND_REQUEST));

        if (!friend.getRequester().getId().equals(memberId) && !friend.getReceiver().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.HAS_NOT_FRIEND_AUTHORIZATION);
        }

        if (friend.getStatus() != FriendStatus.ACCEPTED) {
            throw new MyException(MyErrorCode.NOT_FRIEND);
        }

        friend.updateAlias(memberId, requestDto.getAlias());
    }

    public String getFriendAlias(Long viewerId, Member target) {
        Member viewer = memberRepository.findById(viewerId).orElse(null);
        if (viewer == null || target == null) return null;

        return friendRepository.findByRequesterAndReceiver(viewer, target)
                .filter(f -> f.getStatus() == FriendStatus.ACCEPTED)
                .map(Friend::getRequesterAlias)
                .orElseGet(() -> friendRepository.findByRequesterAndReceiver(target, viewer)
                        .filter(f -> f.getStatus() == FriendStatus.ACCEPTED)
                        .map(Friend::getReceiverAlias)
                        .orElse(null));
    }

    @Transactional(readOnly = true)
    public MemberProfileResponseDto getFriendProfile(Long viewerId, Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_FRIEND_REQUEST));

        if (!friend.getRequester().getId().equals(viewerId) && !friend.getReceiver().getId().equals(viewerId)) {
            throw new MyException(MyErrorCode.HAS_NOT_FRIEND_AUTHORIZATION);
        }

        if (friend.getStatus() != FriendStatus.ACCEPTED) {
            throw new MyException(MyErrorCode.NOT_FRIEND);
        }

        Member target = friend.getRequester().getId().equals(viewerId) ? friend.getReceiver() : friend.getRequester();
        Member viewer = friend.getRequester().getId().equals(viewerId) ? friend.getRequester() : friend.getReceiver();

        if (blockRepository.existsByBlockerAndBlocked(target, viewer) ||
                blockRepository.existsByBlockerAndBlocked(viewer, target)) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        String friendAlias = friend.getRequester().getId().equals(viewerId) ? friend.getRequesterAlias() : friend.getReceiverAlias();

        return MemberProfileResponseDto.builder()
                .memberId(target.getId())
                .nickname(target.getNickname())
                .fireId(target.getFireId())
                .department(target.getDepartment())
                .maskedStudentId(target.getMaskedStudentId())
                .friendStatus("ACCEPTED")
                .friendAlias(friendAlias)
                .friendId(friend.getId())
                .build();
    }

    // 친구관계 양방향 확인 메서드
    private boolean isAcceptedFriend(Long memberId, Long targetMemberId) {
        if (memberId == null || targetMemberId == null) {
            return false;
        }


        return friendRepository.existsFriendship(
                memberId,
                targetMemberId,
                FriendStatus.ACCEPTED
        );
    }

    // 친구 관계 및 차단 관계 검증 메서드
    @Transactional(readOnly = true)
    public boolean isReadableFriend(Long memberId, Long targetMemberId) {
        if (!isAcceptedFriend(memberId, targetMemberId)) {
            return false;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        return !blockRepository.existsByBlockerAndBlocked(member, target)
                && !blockRepository.existsByBlockerAndBlocked(target, member);
    }

    private static final int DEFAULT_NEARBY_RADIUS_METERS = 200;
    private static final long NEARBY_LOCATION_TTL_MINUTES = 5L;
    private static final double EARTH_RADIUS_METERS = 6371000.0;

    @Transactional(readOnly = true)
    public List<NearbyFriendResponseDto> getNearbyFriends(Long memberId, Double latitude, Double longitude, Integer radiusMeters) {
        if (latitude == null || longitude == null) {
            throw new MyException(MyErrorCode.REQUIRED_LOCATION_PARAMETER);
        }
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude > 180.0) {
            throw new MyException(MyErrorCode.INVALID_LOCATION_VALUE);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(member.getNearbyVisibility())) {
            throw new MyException(MyErrorCode.NEARBY_VISIBILITY_NOT_AGREED);
        }

        int effectiveRadius = (radiusMeters == null || radiusMeters <= 0) ? DEFAULT_NEARBY_RADIUS_METERS : radiusMeters;
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(NEARBY_LOCATION_TTL_MINUTES);

        List<Member> candidates = memberRepository.findActiveNearbyCandidates(cutoffTime, member.getId());

        List<NearbyFriendResponseDto> result = new ArrayList<>();
        for (Member candidate : candidates) {
            if (isAcceptedFriend(member.getId(), candidate.getId())) {
                continue;
            }
            if (blockRepository.existsByBlockerAndBlocked(member, candidate) ||
                    blockRepository.existsByBlockerAndBlocked(candidate, member)) {
                continue;
            }

            double distance = calculateDistanceInMeters(latitude, longitude, candidate.getLatitude(), candidate.getLongitude());
            long distanceMeters = Math.round(distance);

            if (distanceMeters <= effectiveRadius) {
                result.add(NearbyFriendResponseDto.builder()
                        .memberId(candidate.getId())
                        .nickname(candidate.getNickname())
                        .studentId(candidate.getMaskedStudentId())
                        .fireId(candidate.getFireId())
                        .distanceMeters(distanceMeters)
                        .build());
            }
        }

        result.sort(Comparator.comparingLong(NearbyFriendResponseDto::distanceMeters));
        return result;
    }

    private double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
