package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.dto.FriendAliasRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        fcmAsyncService.sendAsyncTrackedNotification(List.of(receiver.getId()), "친구 요청", requester.getNickname() + "님이 친구 요청을 보냈습니다.", FcmMessageType.FRIEND);
    }

    @Transactional(readOnly = true)
    public List<FriendResponseDto> getPendingRequests(Long memberId) {
        Member receiver = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        
        List<Friend> requests = friendRepository.findAllByReceiverAndStatus(receiver, FriendStatus.PENDING);
        
        return requests.stream().map(f -> FriendResponseDto.builder()
                .friendId(f.getId())
                .memberId(f.getRequester().getId())
                .nickname(f.getRequester().getNickname())
                .studentId(f.getRequester().getMaskedStudentId())
                .fireId(f.getRequester().getFireId())
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
                .memberId(f.getReceiver().getId())
                .nickname(f.getReceiver().getNickname())
                .studentId(f.getReceiver().getMaskedStudentId())
                .fireId(f.getReceiver().getFireId())
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
                .memberId(target.getId())
                .nickname(target.getNickname())
                .studentId(target.getMaskedStudentId())
                .fireId(target.getFireId())
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

        fcmAsyncService.sendAsyncTrackedNotification(List.of(friend.getRequester().getId()), "친구 수락", friend.getReceiver().getNickname() + "님이 친구 요청을 수락했습니다.", FcmMessageType.FRIEND);
    }

    @Transactional
    public void deleteFriend(Long memberId, Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_FRIEND_REQUEST));

        if (!friend.getRequester().getId().equals(memberId) && !friend.getReceiver().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.HAS_NOT_FRIEND_AUTHORIZATION);
        }

        if (friend.getStatus() == FriendStatus.PENDING && friend.getReceiver().getId().equals(memberId)) {
            fcmAsyncService.sendAsyncTrackedNotification(List.of(friend.getRequester().getId()), "친구 요청 결과", "친구 요청이 거절되었습니다.", FcmMessageType.FRIEND);
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
                .memberId(f.getReceiver().getId())
                .nickname(f.getReceiver().getNickname())
                .studentId(f.getReceiver().getMaskedStudentId())
                .fireId(f.getReceiver().getFireId())
                .friendAlias(f.getRequesterAlias())
                .build()
        ).collect(Collectors.toList());

        list.addAll(received.stream().map(f -> FriendResponseDto.builder()
                .friendId(f.getId())
                .memberId(f.getRequester().getId())
                .nickname(f.getRequester().getNickname())
                .studentId(f.getRequester().getMaskedStudentId())
                .fireId(f.getRequester().getFireId())
                .friendAlias(f.getReceiverAlias())
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
}
