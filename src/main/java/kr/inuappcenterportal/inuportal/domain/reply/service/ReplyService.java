package kr.inuappcenterportal.inuportal.domain.reply.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReplyService {
    private final ReplyRepository replyRepository;
    private final PostRepository postRepository;
    private final LikeReplyRepository likeReplyRepository;
    private final BlockRepository blockRepository;
    private final FcmService fcmService;

    @Transactional
    public Long saveReply(Member member, ReplyDto replyDto, Long postId) {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        long num = countAnonymousNumber(member,post);
        Reply reply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).post(post).number(num).build();
        replyRepository.save(reply);
        post.upReplyCount();

        sendReplyNotification(member, post, reply);
        return reply.getId();
    }

    @Transactional
    public Long saveReReply(Member member, ReplyDto replyDto, Long replyId) {
        Reply reply = replyRepository.findByIdAndIsDeletedFalse(replyId).orElseThrow(()->new MyException(MyErrorCode.REPLY_NOT_FOUND));
        if(reply.getReply()!=null){
            throw new MyException(MyErrorCode.NOT_REPLY_ON_REREPLY);
        }
        Post post = postRepository.findByIdAndIsDeletedFalse(reply.getPost().getId()).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        long num = countAnonymousNumber(member,post);
        Reply reReply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).reply(reply).post(post).number(num).build();
        post.upReplyCount();
        Reply savedReReply = replyRepository.save(reReply);

        sendReReplyNotification(member, post, reply, savedReReply);
        return savedReReply.getId();
    }

    private void sendReplyNotification(Member writerMember, Post post, Reply reply) {
        try {
            Member postAuthor = post.getMember();
            if (postAuthor != null && !postAuthor.getId().equals(writerMember.getId())) {
                String title = post.getTitle();
                String body = "댓글이 달렸어요: " + reply.getContent();
                String path = "/home/tips/" + post.getId();

                // prepareTrackedNotification이 저장 트랜잭션 커밋 이후 발송을 이벤트로
                // 트리거한다. 여기서 dispatchTrackedNotification을 또 호출하면 중복 발송된다.
                fcmService.prepareTrackedNotification(
                        List.of(postAuthor.getId()),
                        title,
                        body,
                        FcmMessageType.POST_REPLY,
                        post.getId(),
                        path
                );
            }
        } catch (Exception e) {
            log.error("댓글 FCM 푸시알림 발송 실패: postId={}, replyId={}", post.getId(), reply.getId(), e);
        }
    }

    private void sendReReplyNotification(Member writerMember, Post post, Reply parentReply, Reply reReply) {
        try {
            Set<Long> targetMemberIds = new HashSet<>();

            // 1. 원글 작성자
            if (post.getMember() != null) {
                targetMemberIds.add(post.getMember().getId());
            }

            // 2. 원댓글 작성자
            if (parentReply.getMember() != null) {
                targetMemberIds.add(parentReply.getMember().getId());
            }

            // 3. 해당 원댓글 스레드에 답글을 달았던 참여자들
            List<Member> reReplyMembers = replyRepository.findReReplyMembersByParentReply(parentReply);
            for (Member m : reReplyMembers) {
                if (m != null) {
                    targetMemberIds.add(m.getId());
                }
            }

            // 본인 제외
            targetMemberIds.remove(writerMember.getId());

            if (!targetMemberIds.isEmpty()) {
                String title = post.getTitle();
                String body = "답글이 달렸어요: " + reReply.getContent();
                String path = "/home/tips/" + post.getId();

                // prepareTrackedNotification이 저장 트랜잭션 커밋 이후 발송을 이벤트로
                // 트리거한다. 여기서 dispatchTrackedNotification을 또 호출하면 중복 발송된다.
                fcmService.prepareTrackedNotification(
                        new ArrayList<>(targetMemberIds),
                        title,
                        body,
                        FcmMessageType.POST_REPLY,
                        post.getId(),
                        path
                );
            }
        } catch (Exception e) {
            log.error("답글 FCM 푸시알림 발송 실패: postId={}, reReplyId={}", post.getId(), reReply.getId(), e);
        }
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
        
        List<Long> blockedMemberIds = new ArrayList<>();
        if (member != null) {
            blockedMemberIds = blockRepository.findAllByBlocker(member).stream()
                    .map(b -> b.getBlocked().getId()).collect(Collectors.toList());
        }

        Set<Long> likedReplyIds = getMemberLikedIds(replies,member);
        List<Long> finalBlockedMemberIds = blockedMemberIds;
        return replies.stream()
                .filter(reply -> reply.getReply() == null)
                .filter(reply -> reply.getMember() == null || !finalBlockedMemberIds.contains(reply.getMember().getId())) // 차단 필터
                .map(reply -> {
                    List<ReReplyResponseDto> reReplies = getReReplies(replies,reply,likedReplyIds,post,member, finalBlockedMemberIds);
                    boolean isLiked = likedReplyIds.contains(reply.getId());
                    String writer = writerName(reply,post);
                    long fireId = writer.equals("(알수없음)") ? 13 : reply.getMember().getFireId();
                    return ReplyResponseDto.of(reply, writer, fireId, isLiked, hasAuthority(member, reply), reReplies);
                })
                .collect(Collectors.toList());
    }

    private List<ReReplyResponseDto> getReReplies(List<Reply> replies, Reply reply, Set<Long> likedReplyIds, Post post, Member member, List<Long> blockedMemberIds){
        return  replies.stream()
                .filter(reReply -> reReply.getReply() != null && reReply.getReply().getId().equals(reply.getId()))
                .filter(reReply -> reReply.getMember() == null || !blockedMemberIds.contains(reReply.getMember().getId())) // 차단 필터
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

        List<Long> blockedMemberIds = new ArrayList<>();
        if (member != null) {
            blockedMemberIds = blockRepository.findAllByBlocker(member).stream()
                    .map(b -> b.getBlocked().getId()).collect(Collectors.toList());
        }

        Set<Long> likedReplyIds = getMemberLikedIds(replies,member);
        List<Long> finalBlockedMemberIds = blockedMemberIds;
        return replies.stream()
                .filter(reply -> reply.getMember() == null || !finalBlockedMemberIds.contains(reply.getMember().getId())) // 차단 필터
                .map(reply -> {
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
