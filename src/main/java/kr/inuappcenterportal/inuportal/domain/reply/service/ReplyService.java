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
@Transactional(readOnly = true)
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final PostRepository postRepository;
    private final LikeReplyRepository likeReplyRepository;
    private final RedisService redisService;

    @Transactional
    public Long saveReply(Member member, ReplyDto replyDto, Long postId) throws NoSuchAlgorithmException {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
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
        Reply reply = replyRepository.findByIdAndIsDeletedFalse(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
        String hash = member.getId() + replyDto.getContent();
        redisService.blockRepeat(hash);
        if(reply.getReply()!=null){
            throw new MyException(MyErrorCode.NOT_REPLY_ON_REREPLY);
        }
        Post post = postRepository.findByIdAndIsDeletedFalse(reply.getPost().getId()).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        long num = countAnonymousNumber(member,post);
        Reply reReply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).reply(reply).post(post).number(num).build();
        post.upReplyCount();
        return replyRepository.save(reReply).getId();
    }


    private long countAnonymousNumber(Member member, Post post){
        if (isSamePostAuthor(post,member)) {
            return 0;
        }
        return replyRepository.findFirstByMemberAndPost(member, post)
                .map(Reply::getNumber)
                .orElseGet(() -> {
                    post.upNumber();
                    return post.getNumber();
                });
    }

    private boolean isSamePostAuthor(Post post, Member member) {
        return post.getMember() != null && member.getId().equals(post.getMember().getId());
    }


    @Transactional
    public Long updateReply(Long memberId, ReplyDto replyDto, Long replyId){
        Reply reply = replyRepository.findByIdAndIsDeletedFalse(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
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
        Reply reply = replyRepository.findByIdAndIsDeletedFalse(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
        Post post = postRepository.findByIdAndIsDeletedFalse(reply.getPost().getId()).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
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
        Reply reply = replyRepository.findByIdWithLock(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
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

    public List<ReplyListResponseDto> getReplyByMember(Member member,String sort){
        return replyRepository.findAllByMemberAndIsDeletedFalse(member,sortReply(sort)).stream().map(ReplyListResponseDto::of).collect(Collectors.toList());
    }

    public List<ReplyResponseDto> getReplies(Long postId, Member member) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        List<Reply> replies = replyRepository.findAllNonDeletedOrHavingChildren(post);
        Set<Long> likedReplyIds = getMemberLikedIds(replies,member);
        return replies.stream()
                .filter(reply -> reply.getReply() == null)
                .map(reply -> {
                    List<ReReplyResponseDto> reReplies = getReReplies(replies,reply,likedReplyIds,post,member);
                    boolean isLiked = likedReplyIds.contains(reply.getId());
                    String writer = writerName(reply,post);
                    long fireId = writer.equals("(알수없음)") ? 13 : reply.getMember().getFireId();
                    return ReplyResponseDto.of(reply, writer, fireId, isLiked, hasAuthority(member, reply), reReplies);
                })
                .collect(Collectors.toList());
    }

    private List<ReReplyResponseDto> getReReplies(List<Reply> replies, Reply reply, Set<Long> likedReplyIds, Post post, Member member){
        return  replies.stream()
                .filter(reReply -> reReply.getReply() != null && reReply.getReply().getId().equals(reply.getId()))
                .map(reReply -> {
                    boolean isLiked = likedReplyIds.contains(reReply.getId());
                    String writer = writerName(reReply,post);
                    long fireId = writer.equals("(알수없음)") ? 13 : reReply.getMember().getFireId();
                    return ReReplyResponseDto.of(reReply, writer, fireId, isLiked, hasAuthority(member, reReply));
                }).collect(Collectors.toList());
    }



    private boolean hasAuthority(Member member, Reply reply){
        boolean hasAuthority = false;
        if(!reply.getIsDeleted()&&member!=null&&reply.getMember()!=null&&reply.getMember().getId().equals(member.getId())){
            hasAuthority = true;
        }
        return hasAuthority;
    }
    private String writerName(Reply reply,Post post){
        if(reply.getIsDeleted()){
            return "(삭제됨)";
        }
        if(reply.getMember()==null){
            return "(알수없음)";
        }
        if (reply.getAnonymous()) {
             if(reply.getMember().equals(post.getMember())){
                 return "횃불이(글쓴이)";
             }
             else {
                 return  "횃불이" + reply.getNumber();
             }
        }
        else{
            return reply.getMember().getNickname();
        }
    }


    public List<ReReplyResponseDto> getBestReplies(Long postId,Member member){
        Post post = postRepository.findByIdAndIsDeletedFalse(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
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
