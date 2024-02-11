package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.*;
import kr.inuappcenterportal.inuportal.dto.PostDto;
import kr.inuappcenterportal.inuportal.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.dto.PostResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotFoundException;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotPermittedException;
import kr.inuappcenterportal.inuportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final LikePostRepository likePostRepository;
    private final ScrapRepository scrapRepository;
    private final ReplyService replyService;

    @Transactional
    public Long save(Long id, PostDto postSaveDto){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = Post.builder().title(postSaveDto.getTitle()).content(postSaveDto.getContent()).anonymous(postSaveDto.getAnonymous()).category(postSaveDto.getCategory()).member(member).build();
        postRepository.save(post);
        return post.getId();
    }

    @Transactional
    public void update(Long memberId, Long postId, PostDto postDto){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        post.update(postDto.getTitle(),postDto.getContent(), postDto.getCategory(),postDto.getAnonymous());
    }

    @Transactional
    public void delete(Long memberId, Long postId){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public PostResponseDto getPost(Long postId,Long memberId){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        Boolean isLiked = false;
        Boolean isScraped = false;
        if(memberId!=-1L){
            Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
            if(likePostRepository.existsByMemberAndPost(member,post)){
                isLiked = true;
            }
            if(scrapRepository.existsByMemberAndPost(member,post)){
                isScraped = true;
            }
        }
        String writer;
        if(post.getMember()==null){
            writer="(알수없음)";
        }
        else{
            if (post.getAnonymous()) {
                writer = "익명";
            }
            else{
                writer = memberRepository.findById(post.getMember().getId()).get().getNickname();
            }
        }
        return PostResponseDto.builder()
                .id(post.getId())
                .replies(replyService.getReplies(postId,memberId))
                .createDate(post.getCreateDate())
                .modifiedDate(post.getModifiedDate())
                .category(post.getCategory())
                .writer(writer)
                .title(post.getTitle())
                .like(post.getPostLikes().size())
                .scrap(post.getScraps().size())
                .isLiked(isLiked)
                .isScraped(isScraped)
                .build();
    }


    @Transactional(readOnly = true)
    public List<PostListResponseDto> getAllPost(){
        return postRepository.findAll().stream()
                .map(this::getPostListResponseDto)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<PostListResponseDto> getPostByCategory(String category){
        return postRepository.findAllByCategory(category).stream()
                .map(this::getPostListResponseDto)
                .sorted(Comparator.comparingInt(PostListResponseDto::getLike).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public int likePost(Long memberId, Long postId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(likePostRepository.existsByMemberAndPost(member,post)){
            PostLike postLike = likePostRepository.findByMemberAndPost(member,post).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_OR_POST_NOT_FOUND));;
            likePostRepository.delete(postLike);
            return -1;
        }
        else {
            PostLike postLike = PostLike.builder().member(member).post(post).build();
            likePostRepository.save(postLike);
            return 1;
        }
    }

    @Transactional
    public int scrapPost(Long memberId, Long postId){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(scrapRepository.existsByMemberAndPost(member,post)){
            Scrap scrap = scrapRepository.findByMemberAndPost(member,post).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_OR_POST_NOT_FOUND));
            scrapRepository.delete(scrap);
            return -1;
        }
        else{
            Scrap scrap = Scrap.builder().member(member).post(post).build();
            scrapRepository.save(scrap);
            return 1;
        }
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getPostByMember(Long memberId){
        Member mem = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        return postRepository.findAllByMember(mem)
                .stream()
                .map(this::getPostListResponseDto)
                .sorted(Comparator.comparingInt(PostListResponseDto::getLike).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getAllScraps(Long memberId){
        Member mem = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        List<Scrap> scraps = scrapRepository.findAllByMember(mem);
        return scraps.stream()
                .map(scrap -> {
                    Post post = scrap.getPost();
                    return getPostListResponseDto(post);
                })
                .collect(Collectors.toList());
    }

    private PostListResponseDto getPostListResponseDto(Post post) {
        String writer;
        if(post.getMember()==null){
            writer="(알수없음)";
        }
        else{
            if (post.getAnonymous()) {
                writer = "익명";
            }
            else{
                writer = memberRepository.findById(post.getMember().getId()).get().getNickname();
            }
        }
        return PostListResponseDto.builder()
                .id(post.getId())
                .createDate(post.getCreateDate())
                .modifiedDate(post.getModifiedDate())
                .category(post.getCategory())
                .writer(writer)
                .title(post.getTitle())
                .like(post.getPostLikes().size())
                .scrap(post.getScraps().size())
                .build();
    }

}
