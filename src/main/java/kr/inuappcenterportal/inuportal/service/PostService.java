package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.*;
import kr.inuappcenterportal.inuportal.dto.PostDto;
import kr.inuappcenterportal.inuportal.dto.PostResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotFoundException;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotPermittedException;
import kr.inuappcenterportal.inuportal.repository.*;
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
    private final ScrapRepository scrapRepository;

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
    public int likePost(Long memberId, Long postId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(likeRepository.existsByMemberAndPost(member,post)){
            Good good = likeRepository.findByMemberAndPost(member,post).get();
            likeRepository.delete(good);
            return -1;
        }
        else {
            Good good = Good.builder().member(member).post(post).build();
            likeRepository.save(good);
            return 1;
        }
    }

    @Transactional
    public int dislikePost(Long memberId, Long postId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(disLikeRepository.existsByMemberAndPost(member,post)){
            DisLike disLike = disLikeRepository.findByMemberAndPost(member,post).get();
            disLikeRepository.delete(disLike);
            return -1;
        }
        else {
            DisLike disLike = DisLike.builder().member(member).post(post).build();
            disLikeRepository.save(disLike);
            return 1;
        }
    }

    @Transactional
    public int scrapPost(Long memberId, Long postId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(scrapRepository.existsByMemberAndPost(member,post)){
            Scrap scrap = scrapRepository.findByMemberAndPost(member,post).get();
            scrapRepository.delete(scrap);
            return -1;
        }
        else{
            Scrap scrap = Scrap.builder().member(member).post(post).build();
            scrapRepository.save(scrap);
            return 1;
        }
    }

}
