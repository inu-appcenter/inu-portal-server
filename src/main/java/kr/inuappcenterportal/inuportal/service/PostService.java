package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.*;
import kr.inuappcenterportal.inuportal.dto.PostDto;
import kr.inuappcenterportal.inuportal.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.dto.PostResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyBadRequestException;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotFoundException;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotPermittedException;
import kr.inuappcenterportal.inuportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final CategoryRepository categoryRepository;
    private final RedisService redisService;


    @Transactional
    public Long saveOnlyPost(Long id, PostDto postSaveDto) {
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        Post post = Post.builder().title(postSaveDto.getTitle()).content(postSaveDto.getContent()).anonymous(postSaveDto.getAnonymous()).category(postSaveDto.getCategory()).member(member).imageCount(0).build();
        postRepository.save(post);
        return post.getId();
    }
    @Transactional
    public Long saveOnlyImage(Long memberId, Long postId, List<MultipartFile> imageDto) throws IOException {
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        if(!post.getMember().getId().equals(member.getId())){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        int imageCount;
        if(imageDto==null){
            imageCount = 0;
        }
        else{
            imageCount = imageDto.size();
        }
        if(imageDto!=null) {
            redisService.saveImage(post.getId(), imageDto);
        }
        post.updateImageCount(imageCount);
        return post.getId();
    }

    @Transactional
    public void updateOnlyPost(Long memberId, Long postId, PostDto postDto){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        post.updateOnlyPost(postDto.getTitle(),postDto.getContent(), postDto.getCategory(),postDto.getAnonymous());
    }

    @Transactional
    public void updateOnlyImage(Long memberId, Long postId, List<MultipartFile> images) throws IOException {
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        redisService.updateImage(postId,images,post.getImageCount());
        post.updateImageCount(images.size());
    }

    @Transactional
    public void delete(Long memberId, Long postId){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        redisService.deleteImage(postId,post.getImageCount());
        if(!post.getMember().getId().equals(memberId)){
            throw new MyNotPermittedException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        postRepository.delete(post);
    }

    @Transactional
    public PostResponseDto getPost(Long postId,Long memberId,String address){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyNotFoundException(MyErrorCode.POST_NOT_FOUND));
        if(redisService.isFirstConnect(address,postId)){
            redisService.insertAddress(address,postId);
            post.upViewCount();
        }
        boolean isLiked = false;
        boolean isScraped = false;
        boolean hasAuthority = false;
        if(memberId!=-1L){
            Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
            if(likePostRepository.existsByMemberAndPost(member,post)){
                isLiked = true;
            }
            if(scrapRepository.existsByMemberAndPost(member,post)){
                isScraped = true;
            }
            if(post.getMember()!=null&&member.getId().equals(post.getMember().getId())){
                hasAuthority = true;
            }
        }
        String writer;
        if(post.getMember()==null){
            writer="(알수없음)";
        }
        else{
            if (post.getAnonymous()) {
                writer = "횃불이";
            }
            else{
                writer = memberRepository.findById(post.getMember().getId()).get().getNickname();
            }
        }

        return PostResponseDto.builder()
                .id(post.getId())
                .replies(replyService.getReplies(postId,memberId))
                .bestReplies(replyService.getBestReplies(postId,memberId))
                .createDate(post.getCreateDate())
                .modifiedDate(post.getModifiedDate())
                .category(post.getCategory())
                .writer(writer)
                .title(post.getTitle())
                .content(post.getContent())
                .like(post.getPostLikes().size())
                .scrap(post.getScraps().size())
                .isLiked(isLiked)
                .isScraped(isScraped)
                .hasAuthority(hasAuthority)
                .view(post.getView())
                .imageCount(post.getImageCount())
                .build();
    }


    @Transactional(readOnly = true)
    public List<PostListResponseDto> getAllPost(String category, String sort){
        if(category==null){
            List<PostListResponseDto> posts = postRepository.findAllByOrderByIdDesc().stream()
                    .map(this::getPostListResponseDto)
                    .collect(Collectors.toList());
            return postListSort(posts, sort);
        }
        else{
            if(!categoryRepository.existsByCategory(category)){
                throw new MyNotFoundException(MyErrorCode.CATEGORY_NOT_FOUND);
            }
            List<PostListResponseDto> posts =  postRepository.findAllByCategoryOrderByIdDesc(category).stream()
                    .map(this::getPostListResponseDto)
                    .collect(Collectors.toList());
           return postListSort(posts,sort);
        }

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
    public List<PostListResponseDto> getPostByMember(Long memberId, String sort){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        if(sort==null) {
            return postRepository.findAllByMemberOrderByIdDesc(member)
                    .stream()
                    .map(this::getPostListResponseDto)
                    .collect(Collectors.toList());
        }
        else if(sort.equals("like")){
            return postRepository.findAllByMemberOrderByIdDesc(member)
                    .stream()
                    .map(this::getPostListResponseDto)
                    .sorted(Comparator.comparing(PostListResponseDto::getLike).reversed())
                    .collect(Collectors.toList());
        }
        else{
            throw new MyBadRequestException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getScrapsByMember(Long memberId, String sort){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        List<PostListResponseDto> scraps = scrapRepository.findAllByMember(member).stream()
                .map(scrap -> {
                    Post post = scrap.getPost();
                    return getPostListResponseDto(post);
                })
                .sorted(Comparator.comparing(PostListResponseDto::getId).reversed())
                .collect(Collectors.toList());
        return  postListSort(scraps,sort);
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getLikeByMember(Long memberId, String sort){
        Member member = memberRepository.findById(memberId).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        List<PostListResponseDto> likes = likePostRepository.findAllByMember(member).stream()
                .map(like -> {
                    Post post = like.getPost();
                    return getPostListResponseDto(post);
                })
                .sorted(Comparator.comparing(PostListResponseDto::getId).reversed())
                .collect(Collectors.toList());
        return postListSort(likes,sort);
    }

    public List<PostListResponseDto> postListSort(List<PostListResponseDto> posts,String sort){
        if(sort==null){
            return posts;
        }
        else if(sort.equals("like")){
            posts.sort((o1, o2) -> o2.getLike() - o1.getLike());
            return posts;
        }

        else{
            throw new MyBadRequestException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }


    public PostListResponseDto getPostListResponseDto(Post post) {
        String writer;
        if(post.getMember()==null){
            writer="(알수없음)";
        }
        else{
            if (post.getAnonymous()) {
                writer = "횃불이";
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
                .content((post.getContent().length()>50)?post.getContent().substring(0,50)+"...":post.getContent())
                .title(post.getTitle())
                .like(post.getPostLikes().size())
                .imageCount(post.getImageCount())
                .scrap(post.getScraps().size())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> searchPost(String query,String sort){
            if (sort == null) {
                return postRepository.findAllByTitleContainsOrContentContainsOrderByIdDesc(query,query).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
            } else if (sort.equals("view")) {
                return postRepository.findAllByTitleContainsOrContentContainsOrderByViewDesc(query,query).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
            } else if (sort.equals("like")) {
                return postRepository.findAllByTitleContainsOrContentContains(query,query).stream().map(this::getPostListResponseDto).sorted(Comparator.comparingInt(PostListResponseDto::getLike).reversed()).collect(Collectors.toList());
            } else if (sort.equals("scrap")) {
                return postRepository.findAllByTitleContainsOrContentContains(query,query).stream().map(this::getPostListResponseDto).sorted(Comparator.comparingInt(PostListResponseDto::getScrap).reversed()).collect(Collectors.toList());
            } else {
                throw new MyBadRequestException(MyErrorCode.WRONG_SORT_TYPE);
            }
        }

}
