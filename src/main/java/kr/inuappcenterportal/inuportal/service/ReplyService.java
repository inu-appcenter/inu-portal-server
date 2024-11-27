package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.domain.Reply;
import kr.inuappcenterportal.inuportal.domain.ReplyLike;
import kr.inuappcenterportal.inuportal.dto.ReReplyResponseDto;
import kr.inuappcenterportal.inuportal.dto.ReplyDto;
import kr.inuappcenterportal.inuportal.dto.ReplyListResponseDto;
import kr.inuappcenterportal.inuportal.dto.ReplyResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.repository.LikeReplyRepository;
import kr.inuappcenterportal.inuportal.repository.PostRepository;
import kr.inuappcenterportal.inuportal.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
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
        long num = countNumber(member,post);
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
        long num = countNumber(member,post);
        Reply reReply = Reply.builder().content(replyDto.getContent()).anonymous(replyDto.getAnonymous()).member(member).reply(reply).post(post).number(num).build();
        post.upReplyCount();
        return replyRepository.save(reReply).getId();
    }

    @Transactional
    public long countNumber(Member member, Post post){
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
            if(replyRepository.existsByReply(reply)) {
                reply.onDelete("삭제된 댓글입니다.", null);
            }
            else replyRepository.delete(reply);

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
            return -1;
        }
        else {
            ReplyLike replyLike = ReplyLike.builder().member(member).reply(reply).build();
            likeReplyRepository.save(replyLike);
            return 1;
        }
    }

    @Transactional(readOnly = true)
    public List<ReplyListResponseDto> getReplyByMember(Member member,String sort){
        if(sort==null||sort.equals("date")) {
            return replyRepository.findAllByMemberOrderByIdDesc(member).stream().map(ReplyListResponseDto::of).collect(Collectors.toList());
        }
        else if(sort.equals("like")){
            return replyRepository.findAllByMemberOrderByIdDesc(member).stream().map(ReplyListResponseDto::of).sorted(Comparator.comparing(ReplyListResponseDto::getLike).reversed()).collect(Collectors.toList());
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

    @Transactional(readOnly = true)
    public List<ReplyResponseDto> getReplies(Long postId, Member member){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        List<Reply> replies = replyRepository.findAllByPostAndReplyIsNull(post);
        return replies.stream().map(reply -> {
            List<ReReplyResponseDto> reReplyResponseDtoList = replyRepository.findAllByReply(reply).stream().map(reReply ->{
                        String writer = writerName(reReply,post);
                        boolean isLiked = isLiked(member,reReply);
                        boolean hasAuthority = hasAuthority(member,reReply);
                        long fireId = writer.equals("(알수없음)")||writer.equals("(삭제됨)")?13: reReply.getMember().getFireId();
                return ReReplyResponseDto.of(reReply,writer,fireId,isLiked,hasAuthority);
            })
                    .collect(Collectors.toList());
                    String writer = writerName(reply,post);
                    long fireId = writer.equals("(알수없음)")||writer.equals("(삭제됨)")?13: reply.getMember().getFireId();
                    boolean isLiked = isLiked(member,reply);
                    boolean hasAuthority = hasAuthority(member,reply);
            return ReplyResponseDto.of(reply, writer, fireId, isLiked,hasAuthority,reReplyResponseDtoList);

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
        List<Reply> list = replyRepository.findAllByPost(post);
        List<Reply> likeList= new ArrayList<>();
        for(Reply reply:list){
            if(!reply.getLikeReplies().isEmpty()){
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
            long fireId = writer.equals("(알수없음)")||writer.equals("(삭제됨)")?13: reply.getMember().getFireId();
            boolean isLiked = isLiked(member,reply);
            boolean hasAuthority = hasAuthority(member,reply);
            bestReplies.add(ReReplyResponseDto.of(reply,writer,fireId, isLiked,hasAuthority));

        }
        return bestReplies;
    }

}
