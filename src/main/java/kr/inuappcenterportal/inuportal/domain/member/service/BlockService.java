package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.dto.BlockResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Block;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.reply.model.Reply;
import kr.inuappcenterportal.inuportal.domain.reply.repository.ReplyRepository;
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
    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;

    @Transactional
    public void blockUser(Long memberId, Long targetMemberId) {
        Member blocker = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        Member blocked = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        blockUser(blocker, blocked);
    }

    /**
     * 게시글 작성자 차단. 익명 글도 작성자(member)는 항상 서버에 남아있으므로 차단 자체는
     * 가능하다 - 다만 PostResponseDto/PostListResponseDto는 memberId를 클라이언트에 절대
     * 내려주지 않는다(글쓴이 재식별로 익명성이 깨지는 것을 막기 위해). 그래서 클라이언트가
     * memberId를 몰라도 postId만으로 차단할 수 있도록 이 경로를 따로 둔다.
     */
    @Transactional
    public void blockUserByPostId(Long memberId, Long postId) {
        Member blocker = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findByIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new MyException(MyErrorCode.POST_NOT_FOUND));

        Member blocked = post.getMember();
        if (blocked == null) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        blockUser(blocker, blocked);
    }

    /**
     * 댓글/대댓글 작성자 차단. 사유는 blockUserByPostId와 동일 - Reply/ReReplyResponseDto도
     * memberId를 내려주지 않는다.
     */
    @Transactional
    public void blockUserByReplyId(Long memberId, Long replyId) {
        Member blocker = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));

        Reply reply = replyRepository.findByIdAndIsDeletedFalse(replyId)
                .orElseThrow(() -> new MyException(MyErrorCode.REPLY_NOT_FOUND));

        Member blocked = reply.getMember();
        if (blocked == null) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        blockUser(blocker, blocked);
    }

    private void blockUser(Member blocker, Member blocked) {
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
