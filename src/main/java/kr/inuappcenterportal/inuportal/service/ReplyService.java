package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.*;
import kr.inuappcenterportal.inuportal.dto.ReReplyResponseDto;
import kr.inuappcenterportal.inuportal.dto.ReplyDto;
import kr.inuappcenterportal.inuportal.dto.ReplyListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ReplyResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotFoundException;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotPermittedException;
import kr.inuappcenterportal.inuportal.repository.LikeReplyRepository;
import kr.inuappcenterportal.inuportal.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.repository.PostRepository;
import kr.inuappcenterportal.inuportal.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final LikeReplyRepository likeReplyRepository;

    @Transactional
    public Long saveReply(Long memberId, ReplyDto replyDto, Long postId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        int num = countNumber(member,post);
        Reply reply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).post(post).number(num).build();
        replyRepository.save(reply);
        return reply.getId();
    }

    @Transactional
    public Long saveReReply(Long memberId, ReplyDto replyDto, Long replyId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
        if(reply.getReply()!=null){
            throw new MyNotPermittedException(MyErrorCode.NOT_REPLY_ON_REREPLY);
        }
        Post post = postRepository.findById(reply.getPost().getId()).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        int num = countNumber(member,post);
        Reply reReply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).reply(reply).post(post).number(num).build();
        return replyRepository.save(reReply).getId();
    }

    @Transactional
    public int countNumber(Member member, Post post){
        int num = 0;
        if(post.getMember()!=null) {
            if (!member.getId().equals(post.getMember().getId()) && replyRepository.existsByMember(member)) {
                Reply preReply = replyRepository.findFirstByMember(member).orElseThrow(() -> new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
                num = preReply.getNumber();
            } else if (!member.getId().equals(post.getMember().getId())) {
                post.upNumber();
                num = post.getNumber();
            }
        }
        else{
            if( replyRepository.existsByMember(member)){
                Reply preReply = replyRepository.findFirstByMember(member).orElseThrow(() -> new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
                num = preReply.getNumber();
            }
            else{
                post.upNumber();
                num = post.getNumber();
            }
        }
        return  num;
    }


    @Transactional
    public Long updateReply(Long memberId, ReplyDto replyDto, Long replyId){
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
        if(!reply.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_REPLY_AUTHORIZATION);
        }
        else{
            reply.update(replyDto.getContent(), replyDto.getAnonymous());
            return reply.getId();
        }
    }

    @Transactional
    public void delete(Long memberId, Long replyId){
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
        if(!reply.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_REPLY_AUTHORIZATION);
        }
        else{
            if(replyRepository.existsByReply(reply)) {
                reply.onDelete("삭제된 댓글입니다.", null);
            }
            else replyRepository.delete(reply);

        }
    }


    @Transactional
    public int likeReply(Long memberId, Long replyId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
        if(likeReplyRepository.existsByMemberAndReply(member,reply)){
            ReplyLike replyLike = likeReplyRepository.findByMemberAndReply(member,reply).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_OR_REPLY_NOT_FOUND));
            likeReplyRepository.delete(replyLike);
            return -1;
        }
        else {
            ReplyLike replyLike = ReplyLike.builder().member(member).reply(reply).build();
            likeReplyRepository.save(replyLike);
            return 1;
        }
    }

    @Transactional(readOnly = true)
    public List<ReplyListResponseDto> getReplyByMember(Long memberId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        return replyRepository.findAllByMember(member).stream().map(ReplyListResponseDto::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getReplies(Long postId, Long memberId){
        Member member = memberRepository.findById(memberId).orElse(null);
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        List<Reply> replies = replyRepository.findAllByPostAndReplyIsNull(post);
        return replies.stream().map(reply -> {
            List<ReReplyResponseDto> reReplyResponseDtoList = replyRepository.findAllByReply(reply).stream().map(reReply ->{
                        String writer = writerName(reReply,post);
                        boolean isLiked = isLiked(member,reReply);
                        boolean hasAuthority = hasAuthority(member,reReply);
                return ReReplyResponseDto.builder()
                        .id(reReply.getId())
                        .writer(writer)
                        .content(reReply.getContent())
                        .like(reReply.getLikeReplies().size())
                        .createDate(reReply.getCreateDate())
                        .modifiedDate(reReply.getModifiedDate())
                        .isLiked(isLiked)
                        .hasAuthority(hasAuthority)
                        .isAnonymous(reReply.getAnonymous())
                        .build();
            })
                    .collect(Collectors.toList());
                    String writer = writerName(reply,post);
                    boolean isLiked = isLiked(member,reply);
                    boolean hasAuthority = hasAuthority(member,reply);
            return ReplyResponseDto.builder()
                    .id(reply.getId())
                    .writer(writer)
                    .content(reply.getContent())
                    .like(reply.getLikeReplies().size())
                    .createDate(reply.getCreateDate())
                    .modifiedDate(reply.getModifiedDate())
                    .reReplies(reReplyResponseDtoList)
                    .isLiked(isLiked)
                    .isAnonymous(reply.getAnonymous())
                    .hasAuthority(hasAuthority)
                    .build();

        })
                .collect(Collectors.toList());
    }
    public boolean isLiked(Member member,Reply reply){
        boolean isLiked = false;
        if(member!=null&&likeReplyRepository.existsByMemberAndReply(member,reply)){
            isLiked = true;
        }
        return isLiked;
    }
    public boolean hasAuthority(Member member, Reply reply){
        boolean hasAuthority = false;
        if(member!=null&&reply.getMember()!=null&&reply.getMember().getId().equals(member.getId())){
            hasAuthority = true;
        }
        return hasAuthority;
    }

    public String writerName(Reply reply,Post post){
        String writer;
        if(reply.getIsDeleted()){
            writer="(삭제됨)";
        }
        else if(reply.getMember()==null){
            writer="(알수없음)";
        }
        else{
            if (reply.getAnonymous()) {
                if(reply.getMember().equals(post.getMember())){
                    writer = "횃불이(글쓴이)";
                }
                else {
                    writer = "횃불이"+reply.getNumber();
                }
            }
            else{
                writer = memberRepository.findById(reply.getMember().getId()).get().getNickname();
            }
        }
        return writer;
    }

    @Transactional(readOnly = true)
    public List<ReReplyResponseDto> getBestReplies(Long postId,Long memberId){
        Member member = memberRepository.findById(memberId).orElse(null);
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        List<Reply> list = replyRepository.findAllByPost(post);
        List<Reply> likeList= new ArrayList<>();
        for(Reply reply:list){
            if(reply.getLikeReplies().size()>0){
                likeList.add(reply);
            }
        }
        likeList.sort((o1, o2) -> o2.getLikeReplies().size() - o1.getLikeReplies().size());
        List<ReReplyResponseDto> bestReplies = new ArrayList<>();
        int count =0;
        for(Reply reply:likeList){
            if(count>2)
                break;
            count++;
            String writer = writerName(reply,post);
            boolean isLiked = isLiked(member,reply);
            boolean hasAuthority = hasAuthority(member,reply);
            bestReplies.add(ReReplyResponseDto.builder()
                    .id(reply.getId())
                    .writer(writer)
                    .content(reply.getContent())
                    .like(reply.getLikeReplies().size())
                    .createDate(reply.getCreateDate())
                    .modifiedDate(reply.getModifiedDate())
                    .isLiked(isLiked)
                    .isAnonymous(reply.getAnonymous())
                    .hasAuthority(hasAuthority)
                    .build());

        }
        return bestReplies;
    }

}
