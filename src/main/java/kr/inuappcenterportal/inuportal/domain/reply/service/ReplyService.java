package kr.inuappcenterportal.inuportal.domain.reply.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.reply.dto.ReReplyResponseDto;
import kr.inuappcenterportal.inuportal.domain.reply.dto.ReplyDto;
import kr.inuappcenterportal.inuportal.domain.reply.dto.ReplyListResponseDto;
import kr.inuappcenterportal.inuportal.domain.reply.dto.ReplyResponseDto;
import kr.inuappcenterportal.inuportal.domain.reply.model.Reply;
import kr.inuappcenterportal.inuportal.domain.reply.repository.ReplyRepository;
import kr.inuappcenterportal.inuportal.domain.replylike.model.ReplyLike;
import kr.inuappcenterportal.inuportal.domain.replylike.repository.LikeReplyRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final PostRepository postRepository;
    private final LikeReplyRepository likeReplyRepository;
    private final RedisService redisService;

    @Transactional
    public Long saveReply(Member member, ReplyDto replyDto, Long postId) throws NoSuchAlgorithmException {
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        String hash = member.getId() + replyDto.getContent();
        redisService.blockRepeat(hash);
        long num = countAnonymousNumber(member,post);
        Reply reply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).post(post).number(num).build();
        replyRepository.save(reply);
        post.upReplyCount();
        return reply.getId();
    }

    @Transactional
    public Long saveReReply(Member member, ReplyDto replyDto, Long replyId) throws NoSuchAlgorithmException {
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
        String hash = member.getId() + replyDto.getContent();
        redisService.blockRepeat(hash);
        if(reply.getReply()!=null){
            throw new MyException(MyErrorCode.NOT_REPLY_ON_REREPLY);
        }
        Post post = postRepository.findById(reply.getPost().getId()).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        long num = countAnonymousNumber(member,post);
        Reply reReply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).reply(reply).post(post).number(num).build();
        post.upReplyCount();
        return replyRepository.save(reReply).getId();
    }

    @Transactional
    public long countAnonymousNumber(Member member, Post post){
        long num = 0;
        if(post.getMember()!=null) {
            if (!member.getId().equals(post.getMember().getId()) && replyRepository.existsByMember(member)) {
                Reply preReply = replyRepository.findFirstByMember(member).orElseThrow(() -> new MyException(MyErrorCode.REPLY_NOT_FOUND));
                num = preReply.getNumber();
            } else if (!member.getId().equals(post.getMember().getId())) {
                post.upNumber();
                num = post.getNumber();
            }
        }
        else{
            if( replyRepository.existsByMember(member)){
                Reply preReply = replyRepository.findFirstByMember(member).orElseThrow(() -> new MyException(MyErrorCode.REPLY_NOT_FOUND));
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
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
        if(!reply.getMember().getId().equals(memberId)){
            throw new MyException(MyErrorCode.HAS_NOT_REPLY_AUTHORIZATION);
        }
        else{
            reply.update(replyDto.getContent(), replyDto.getAnonymous());
            return reply.getId();
        }
    }

    @Transactional
    public void delete(Long memberId, Long replyId){
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
        Post post = postRepository.findById(reply.getPost().getId()).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(!reply.getMember().getId().equals(memberId)){
            throw new MyException(MyErrorCode.HAS_NOT_REPLY_AUTHORIZATION);
        }
        else{
            post.downReplyCount();
            reply.onDelete();
        }
    }


    @Transactional
    public int likeReply(Member member, Long replyId){
        Reply reply = replyRepository.findById(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
        if(reply.getMember()!=null&&reply.getMember().getId().equals(member.getId())){
            throw new MyException(MyErrorCode.NOT_LIKE_MY_REPLY);
        }
        if(likeReplyRepository.existsByMemberAndReply(member,reply)){
            ReplyLike replyLike = likeReplyRepository.findByMemberAndReply(member,reply).orElseThrow(()->new MyException(MyErrorCode.USER_OR_REPLY_NOT_FOUND));
            likeReplyRepository.delete(replyLike);
            reply.downLike();
            return -1;
        }
        else {
            ReplyLike replyLike = ReplyLike.builder().member(member).reply(reply).build();
            likeReplyRepository.save(replyLike);
            reply.upLike();
            return 1;
        }
    }

    @Transactional(readOnly = true)
    public List<ReplyListResponseDto> getReplyByMember(Member member,String sort){
        return replyRepository.findAllByMemberAndIsDeletedFalse(member,sortReply(sort)).stream().map(ReplyListResponseDto::of).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getReplies(Long postId, Member member) {
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        List<Reply> replies = replyRepository.findAllNonDeletedOrHavingChildren(post);
        Set<Long> likedReplyIds = getMemberLikedIds(replies,member);
        return replies.stream()
                .filter(reply -> reply.getReply() == null)
                .map(reply -> {
                    List<ReReplyResponseDto> reReplies = replies.stream()
                            .filter(reReply -> reReply.getReply() != null && reReply.getReply().getId().equals(reply.getId()))
                            .map(reReply -> {
                                boolean isLiked = likedReplyIds.contains(reReply.getId());
                                String writer = writerName(reReply,post);
                                long fireId = writer.equals("(알수없음)") ? 13 : reReply.getMember().getFireId();
                                return ReReplyResponseDto.of(reReply, writer, fireId, isLiked, hasAuthority(member, reReply));
                            }).collect(Collectors.toList());
                    boolean isLiked = likedReplyIds.contains(reply.getId());
                    String writer = writerName(reply,post);
                    long fireId = writer.equals("(알수없음)") ? 13 : reply.getMember().getFireId();
                    return ReplyResponseDto.of(reply, writer, fireId, isLiked, hasAuthority(member, reply), reReplies);
                })
                .collect(Collectors.toList());
    }

    public boolean hasAuthority(Member member, Reply reply){
        boolean hasAuthority = false;
        if(!reply.getIsDeleted()&&member!=null&&reply.getMember()!=null&&reply.getMember().getId().equals(member.getId())){
            hasAuthority = true;
        }
        return hasAuthority;
    }
    @Transactional(readOnly = true)
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
                writer = reply.getMember().getNickname();
            }
        }
        return writer;
    }


    @Transactional(readOnly = true)
    public List<ReReplyResponseDto> getBestReplies(Long postId,Member member){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        List<Reply> replies = replyRepository.findBestReplies(post);
        Set<Long> likedReplyIds = getMemberLikedIds(replies,member);
        return replies.stream().map(reply -> {
            String writer = writerName(reply,post);
            long fireId = writer.equals("(알수없음)")||writer.equals("(삭제됨)")?13: reply.getMember().getFireId();
            boolean isLiked = likedReplyIds.contains(reply.getId());
            boolean hasAuthority = hasAuthority(member,reply);
            return ReReplyResponseDto.of(reply,writer,fireId, isLiked,hasAuthority);
        }).collect(Collectors.toList());
    }

    private Set<Long> getMemberLikedIds(List<Reply> replies, Member member){
        Set<Long> likedReplyIds = new HashSet<>();
        if (member != null) {
            List<Long> replyIds = replies.stream()
                    .map(Reply::getId)
                    .collect(Collectors.toList());
            likedReplyIds.addAll(likeReplyRepository.findLikedReplyIdsByMember(member, replyIds));
        }
        return likedReplyIds;
    }

    private Sort sortReply(String sort){
        if(sort.equals("date")){
            return Sort.by(Sort.Direction.DESC, "id");
        }
        else if(sort.equals("like")){
            return Sort.by(Sort.Direction.DESC, "good","id");
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

}
