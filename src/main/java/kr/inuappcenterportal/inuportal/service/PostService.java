package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.DisLike;
import kr.inuappcenterportal.inuportal.domain.Good;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.domain.Post;
import kr.inuappcenterportal.inuportal.dto.PostDto;
import kr.inuappcenterportal.inuportal.dto.PostResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyDuplicateException;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotFoundException;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotPermittedException;
import kr.inuappcenterportal.inuportal.repository.DisLikeRepository;
import kr.inuappcenterportal.inuportal.repository.LikeRepository;
import kr.inuappcenterportal.inuportal.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final LikeRepository likeRepository;
    private final DisLikeRepository disLikeRepository;

    @Transactional
    public Long save(Long id, PostDto postSaveDto){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = Post.builder().title(postSaveDto.getTitle()).content(postSaveDto.getContent()).category(postSaveDto.getCategory()).member(member).build();
        postRepository.save(post);
        return post.getId();
    }

    @Transactional
    public void update(Long memberId, Long postId, PostDto postDto){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_AUTHORIZATION);
        }
        post.update(postDto.getTitle(),postDto.getContent(), postDto.getCategory());
    }

    @Transactional
    public void delete(Long memberId, Long postId){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_AUTHORIZATION);
        }
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long id){
        Post post = postRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        return new PostResponseDto(post);
    }

    @Transactional
    public void likePost(Long memberId, Long postId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(likeRepository.existsByMemberAndPost(member,post)){
            throw new MyDuplicateException(MyErrorCode.NOT_MULTIPLE_LIKE);
        }
        Good good = Good.builder().member(member).post(post).build();
        likeRepository.save(good);
    }

    @Transactional
    public void dislikePost(Long memberId, Long postId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(disLikeRepository.existsByMemberAndPost(member,post)){
            throw new MyDuplicateException(MyErrorCode.NOT_MULTIPLE_LIKE);
        }
        DisLike disLike = DisLike.builder().member(member).post(post).build();
        disLikeRepository.save(disLike);
    }

}
