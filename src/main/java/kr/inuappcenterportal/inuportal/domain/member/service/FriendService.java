package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.dto.FriendRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
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

    @Transactional
    public void requestFriend(Long memberId, FriendRequestDto requestDto) {
        Member requester = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        
        Member receiver = memberRepository.findByStudentId(requestDto.getStudentId())
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        if (requester.getId().equals(receiver.getId())) {
            throw new MyException(MyErrorCode.BAD_REQUEST_FIRE_AI); // 적절한 에러코드가 없어 임시 사용 (본인 신청 불가)
        }

        if (friendRepository.existsByRequesterAndReceiver(requester, receiver) || 
            friendRepository.existsByRequesterAndReceiver(receiver, requester)) {
            throw new MyException(MyErrorCode.BAD_REQUEST_FIRE_AI); // 이미 친구거나 요청 중
        }

        Friend friend = Friend.builder()
                .requester(requester)
                .receiver(receiver)
                .status(FriendStatus.PENDING)
                .build();
        friendRepository.save(friend);
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
                .studentId(f.getRequester().getStudentId())
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
                .studentId(f.getReceiver().getStudentId())
                .fireId(f.getReceiver().getFireId())
                .build()
        ).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FriendResponseDto searchMemberByStudentId(String studentId) {
        Member member = memberRepository.findByStudentId(studentId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        return FriendResponseDto.builder()
                .memberId(member.getId())
                .nickname(member.getNickname())
                .studentId(member.getStudentId())
                .fireId(member.getFireId())
                .build();
    }

    @Transactional
    public void acceptFriend(Long memberId, Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new MyException(MyErrorCode.BAD_REQUEST_FIRE_AI));

        if (!friend.getReceiver().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }

        friend.accept();
    }

    @Transactional
    public void deleteFriend(Long memberId, Long friendId) {
        Friend friend = friendRepository.findById(friendId)
                .orElseThrow(() -> new MyException(MyErrorCode.BAD_REQUEST_FIRE_AI));

        if (!friend.getRequester().getId().equals(memberId) && !friend.getReceiver().getId().equals(memberId)) {
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
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
                .studentId(f.getReceiver().getStudentId())
                .fireId(f.getReceiver().getFireId())
                .build()
        ).collect(Collectors.toList());

        list.addAll(received.stream().map(f -> FriendResponseDto.builder()
                .friendId(f.getId())
                .memberId(f.getRequester().getId())
                .nickname(f.getRequester().getNickname())
                .studentId(f.getRequester().getStudentId())
                .fireId(f.getRequester().getFireId())
                .build()
        ).collect(Collectors.toList()));

        return list;
    }
}
