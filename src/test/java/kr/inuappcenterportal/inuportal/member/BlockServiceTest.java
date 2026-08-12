package kr.inuappcenterportal.inuportal.member;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.BlockService;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.reply.model.Reply;
import kr.inuappcenterportal.inuportal.domain.reply.repository.ReplyRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PostResponseDto/ReplyResponseDto가 memberId를 내려주지 않아(#294 관련,
 * 익명 글/댓글 재식별 방지) 클라이언트가 postId/replyId만으로 작성자를 차단할 수
 * 있어야 한다는 요구사항을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @Mock
    private BlockRepository blockRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private FriendRepository friendRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private ReplyRepository replyRepository;

    @InjectMocks
    private BlockService blockService;

    private Member memberWithId(long id, String studentId) {
        Member member = Member.builder().studentId(studentId).roles(List.of("ROLE_USER")).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    @DisplayName("postId만으로 게시글 작성자를 차단할 수 있다 (익명 글 포함)")
    void blockUserByPostId_success() {
        Member blocker = memberWithId(1L, "202000001");
        Member writer = memberWithId(2L, "202000002");
        Post anonymousPost = Post.builder()
                .title("t").content("c").category("free").anonymous(true).member(writer).imageCount(0).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(blocker));
        when(postRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(anonymousPost));
        when(blockRepository.existsByBlockerAndBlocked(blocker, writer)).thenReturn(false);
        when(friendRepository.findByRequesterAndReceiver(any(), any())).thenReturn(Optional.empty());

        blockService.blockUserByPostId(1L, 10L);

        verify(blockRepository).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 게시글이면 POST_NOT_FOUND")
    void blockUserByPostId_postNotFound() {
        Member blocker = memberWithId(1L, "202000001");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(blocker));
        when(postRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.blockUserByPostId(1L, 999L))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(MyErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 글을 차단 시도하면 NOT_SELF_BLOCK, block은 저장되지 않는다")
    void blockUserByPostId_selfBlockRejected() {
        Member self = memberWithId(1L, "202000001");
        Post ownPost = Post.builder()
                .title("t").content("c").category("free").anonymous(false).member(self).imageCount(0).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(self));
        when(postRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(ownPost));

        assertThatThrownBy(() -> blockService.blockUserByPostId(1L, 10L))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(MyErrorCode.NOT_SELF_BLOCK);

        verify(blockRepository, never()).save(any());
    }

    @Test
    @DisplayName("replyId만으로 댓글 작성자를 차단할 수 있다 (익명 댓글 포함)")
    void blockUserByReplyId_success() {
        Member blocker = memberWithId(1L, "202000001");
        Member writer = memberWithId(2L, "202000002");
        Post post = Post.builder()
                .title("t").content("c").category("free").anonymous(false).member(writer).imageCount(0).build();
        Reply anonymousReply = new Reply("hi", post, true, writer, null, 1L);

        when(memberRepository.findById(1L)).thenReturn(Optional.of(blocker));
        when(replyRepository.findByIdAndIsDeletedFalse(20L)).thenReturn(Optional.of(anonymousReply));
        when(blockRepository.existsByBlockerAndBlocked(blocker, writer)).thenReturn(false);
        when(friendRepository.findByRequesterAndReceiver(any(), any())).thenReturn(Optional.empty());

        blockService.blockUserByReplyId(1L, 20L);

        verify(blockRepository).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 댓글이면 REPLY_NOT_FOUND")
    void blockUserByReplyId_replyNotFound() {
        Member blocker = memberWithId(1L, "202000001");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(blocker));
        when(replyRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blockService.blockUserByReplyId(1L, 999L))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(MyErrorCode.REPLY_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 차단한 작성자의 다른 글이면 조용히 무시하고 중복 저장하지 않는다")
    void blockUserByPostId_alreadyBlocked_noop() {
        Member blocker = memberWithId(1L, "202000001");
        Member writer = memberWithId(2L, "202000002");
        Post post = Post.builder()
                .title("t").content("c").category("free").anonymous(false).member(writer).imageCount(0).build();

        when(memberRepository.findById(1L)).thenReturn(Optional.of(blocker));
        when(postRepository.findByIdAndIsDeletedFalse(10L)).thenReturn(Optional.of(post));
        when(blockRepository.existsByBlockerAndBlocked(blocker, writer)).thenReturn(true);

        blockService.blockUserByPostId(1L, 10L);

        verify(blockRepository, never()).save(any());
    }
}
