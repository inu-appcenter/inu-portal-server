package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.*;
import kr.inuappcenterportal.inuportal.dto.ReReplyResponseDto;
import kr.inuappcenterportal.inuportal.dto.ReplyDto;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        Integer num = 0;
        if(post.getMember()!=null) {
            if (!member.getId().equals(post.getMember().getId()) && replyRepository.existsByMember(member)) {
                Reply preReply = replyRepository.findByMember(member).orElseThrow(() -> new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
                num = preReply.getNumber();
            } else if (!member.getId().equals(post.getMember().getId())) {
                post.upNumber();
                num = post.getNumber();
            }
        }
        else{
            if( replyRepository.existsByMember(member)){
                Reply preReply = replyRepository.findByMember(member).orElseThrow(() -> new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
                num = preReply.getNumber();
            }
            else{
                post.upNumber();
                num = post.getNumber();
            }
        }
        Reply reply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).post(post).number(num).build();
        replyRepository.save(reply);
        return reply.getId();
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
    public Long saveReReply(Long memberId, ReplyDto replyDto, Long replyId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
        if(reply.getReply()!=null){
            throw new MyNotPermittedException(MyErrorCode.NOT_REPLY_ON_REREPLY);
        }
        Post post = postRepository.findById(reply.getPost().getId()).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        Integer num = 0;
        if(post.getMember()!=null) {
            if (!member.getId().equals(post.getMember().getId()) && replyRepository.existsByMember(member)) {
                Reply preReply = replyRepository.findByMember(member).orElseThrow(() -> new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
                num = preReply.getNumber();
            } else if (!member.getId().equals(post.getMember().getId())) {
                post.upNumber();
                num = post.getNumber();
            }
        }
        else{
            if( replyRepository.existsByMember(member)){
                Reply preReply = replyRepository.findByMember(member).orElseThrow(() -> new MyNotFoundException(MyErrorCode.REPLY_NOT_FOUND));
                num = preReply.getNumber();
            }
            else{
                post.upNumber();
                num = post.getNumber();
            }
        }
        Reply reReply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).reply(reply).post(post).number(num).build();
        return replyRepository.save(reReply).getId();
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
    public List<ReplyResponseDto> getReplies(Long postId, Long memberId){

        Member member = memberRepository.findById(memberId).orElse(null);
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));

        List<Reply> replyList = replyRepository.findAllByPost(post);
        List<Long> idList = replyList.stream().map(list -> list.getMember().getId()).toList();
        Set<Long> idSet = new HashSet<>();
        List<Long> numberList = new ArrayList<>();
        for (Long id : idList) {
            if (idSet.add(id)) {
                numberList.add(id);
            }
        }

        //numberList.removeIf(n ->n.equals(member.getId()));

        List<Reply> replies = replyRepository.findAllByPostAndReplyIsNull(post);
        return replies.stream().map(reply -> {
            List<ReReplyResponseDto> reReplyResponseDtoList = replyRepository.findAllByReply(reply).stream().map(reReply ->{
                        String writer;
                        Boolean isLike = false;
                        if(member!=null&&likeReplyRepository.existsByMemberAndReply(member,reReply)){
                            isLike = true;
                        }
                        if(reReply.getDelete()){
                            writer="(삭제됨)";
                        }
                        else if(reReply.getMember()==null){
                            writer="(알수없음)";
                        }
                        else{
                            if (reReply.getAnonymous()) {
                                if(reReply.getMember().equals(post.getMember())){
                                    writer = "익명(글쓴이)";
                                }
                                else {
                                    writer = "익명"+reReply.getNumber();
                                }
                            }
                            else{
                                writer = memberRepository.findById(reReply.getMember().getId()).get().getNickname();
                            }
                        }
                return ReReplyResponseDto.builder()
                        .id(reReply.getId())
                        .writer(writer)
                        .content(reReply.getContent())
                        .like(reReply.getLikeReplies().size())
                        .createDate(reReply.getCreateDate())
                        .modifiedDate(reReply.getModifiedDate())
                        .isLike(isLike)
                        .build();
            })
                    .collect(Collectors.toList());
                    String writer;
                    Boolean isLike = false;
                    if(member!=null&&likeReplyRepository.existsByMemberAndReply(member,reply)){
                        isLike = true;
                    }
                    if(reply.getDelete()){
                        writer="(삭제됨)";
                    }
                    else if(reply.getMember()==null){
                        writer="(알수없음)";
                    }
                    else{
                        if (reply.getAnonymous()) {
                            if(reply.getMember().equals(post.getMember())){
                                writer = "익명(글쓴이)";
                            }
                            else {
                                writer = "익명"+reply.getNumber();
                            }
                        }
                        else{
                            writer = memberRepository.findById(post.getMember().getId()).get().getNickname();
                        }
                    }
            return ReplyResponseDto.builder()
                    .id(reply.getId())
                    .writer(writer)
                    .content(reply.getContent())
                    .like(reply.getLikeReplies().size())
                    .createDate(reply.getCreateDate())
                    .modifiedDate(reply.getModifiedDate())
                    .reReplies(reReplyResponseDtoList)
                    .isLike(isLike)
                    .build();

        })
                .collect(Collectors.toList());
    }

}
