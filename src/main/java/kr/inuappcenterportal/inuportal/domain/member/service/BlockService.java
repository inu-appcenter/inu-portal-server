package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.dto.BlockResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Block;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
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
public class BlockService {

    private final BlockRepository blockRepository;
    private final MemberRepository memberRepository;
    private final FriendRepository friendRepository;

    @Transactional
    public void blockUser(Long memberId, Long targetMemberId) {
        Member blocker = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        Member blocked = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        if (blocker.getId().equals(blocked.getId())) {
            throw new MyException(MyErrorCode.NOT_SELF_BLOCK);
        }

        if (blockRepository.existsByBlockerAndBlocked(blocker, blocked)) {
            return; // 이미 차단됨
        }

        Block block = Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
        blockRepository.save(block);

        // 친구 관계가 있다면 삭제
        friendRepository.findByRequesterAndReceiver(blocker, blocked).ifPresent(friendRepository::delete);
        friendRepository.findByRequesterAndReceiver(blocked, blocker).ifPresent(friendRepository::delete);
    }

    @Transactional
    public void unblockUser(Long memberId, Long targetMemberId) {
        Member blocker = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        Member blocked = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        blockRepository.findByBlockerAndBlocked(blocker, blocked).ifPresent(blockRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<BlockResponseDto> getBlockList(Long memberId) {
        Member blocker = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        return blockRepository.findAllByBlocker(blocker).stream()
                .map(b -> BlockResponseDto.builder()
                        .blockId(b.getId())
                        .blockedMemberId(b.getBlocked().getId())
                        .nickname(b.getBlocked().getNickname())
                        .studentId(b.getBlocked().getStudentId())
                        .build())
                .collect(Collectors.toList());
    }
}
