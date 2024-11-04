package kr.inuappcenterportal.inuportal.service;

import jakarta.xml.bind.DatatypeConverter;
import kr.inuappcenterportal.inuportal.domain.*;
import kr.inuappcenterportal.inuportal.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.dto.PostDto;
import kr.inuappcenterportal.inuportal.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.dto.PostResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
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
    public Long saveOnlyPost(Member member, PostDto postSaveDto) throws NoSuchAlgorithmException {
        if(!categoryRepository.existsByCategory(postSaveDto.getCategory())){
            throw new MyException(MyErrorCode.CATEGORY_NOT_FOUND);
        }
        String hash = postSaveDto.getTitle()+postSaveDto.getContent();
        redisService.blockRepeat(hash);
        Post post = Post.builder().title(postSaveDto.getTitle()).content(postSaveDto.getContent()).anonymous(postSaveDto.getAnonymous()).category(postSaveDto.getCategory()).member(member).imageCount(0).build();
        postRepository.save(post);
        return post.getId();
    }
    @Transactional
    public Long saveOnlyImage(Member member, Long postId, List<MultipartFile> imageDto) throws IOException {
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(member.getId())){
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        long imageCount;
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
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(!categoryRepository.existsByCategory(postDto.getCategory())){
            throw new MyException(MyErrorCode.CATEGORY_NOT_FOUND);
        }
        if(!post.getMember().getId().equals(memberId)){
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        post.updateOnlyPost(postDto.getTitle(),postDto.getContent(), postDto.getCategory(),postDto.getAnonymous());
    }

    @Transactional
    public void updateOnlyImage(Long memberId, Long postId, List<MultipartFile> images) throws IOException {
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(memberId)){
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        redisService.updateImage(postId,images,post.getImageCount());
        post.updateImageCount(images.size());
    }

    @Transactional
    public void delete(Long memberId, Long postId){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        redisService.deleteImage(postId,post.getImageCount());
        if(!post.getMember().getId().equals(memberId)){
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        postRepository.delete(post);
    }

    @Transactional
    public PostResponseDto getPost(Long postId,Member member,String address){
        Post post = postRepository.findById(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(redisService.isFirstConnect(address,postId)){
            redisService.insertAddress(address,postId);
            post.upViewCount();
        }
        long fireId;
        boolean isLiked = false;
        boolean isScraped = false;
        boolean hasAuthority = false;
        if(member!=null){
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
            fireId =13;
        }
        else{
            fireId = post.getMember().getFireId();
            if (post.getAnonymous()) {
                writer = "횃불이";
            }
            else{
                writer = post.getMember().getNickname();
            }
        }
        return  PostResponseDto.of(post,writer,fireId,isLiked,isScraped,hasAuthority,replyService.getReplies(postId,member),replyService.getBestReplies(postId,member));
    }


    @Transactional(readOnly = true)
    public ListResponseDto getAllPost(String category, String sort, int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8);
        Page<Post> dto;
        if(category==null){
            if(sort==null||sort.equals("date")) {
                dto = postRepository.findAllByOrderByIdDesc(pageable);
            }
            else if(sort.equals("like")){
                dto = postRepository.findAllByOrderByGoodDescIdDesc(pageable);
            }
            else if(sort.equals("scrap")){
                dto = postRepository.findAllByOrderByScrapDescIdDesc(pageable);
            }
            else{
                throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
            }
        }
        else{
            if(!categoryRepository.existsByCategory(category)){
                throw new MyException(MyErrorCode.CATEGORY_NOT_FOUND);
            }
            if(sort==null||sort.equals("date")) {
                dto = postRepository.findAllByCategoryOrderByIdDesc(category, pageable);
            }
            else if(sort.equals("like")){
                dto = postRepository.findAllByCategoryOrderByGoodDescIdDesc(category,pageable);

            }
            else if(sort.equals("scrap")){
                dto = postRepository.findAllByCategoryOrderByScrapDescIdDesc(category,pageable);

            }
            else{
                throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
            }
        }
        long total = dto.getTotalElements();
        long pages = dto.getTotalPages();
        List<PostListResponseDto> posts = dto.stream()
                .map(this::getPostListResponseDto)
                .collect(Collectors.toList());
        return ListResponseDto.of(pages, total, posts);
    }


    @Transactional
    public int likePost(Member member, Long postId){
        Post post = postRepository.findByIdWithLock(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(post.getMember()!=null&&post.getMember().getId().equals(member.getId())){
            throw new MyException(MyErrorCode.NOT_LIKE_MY_POST);
        }
        if(likePostRepository.existsByMemberAndPost(member,post)){
            PostLike postLike = likePostRepository.findByMemberAndPost(member,post).orElseThrow(()->new MyException(MyErrorCode.USER_OR_POST_NOT_FOUND));
            likePostRepository.delete(postLike);
            post.downLike();
            return -1;
        }
        else {
            PostLike postLike = PostLike.builder().member(member).post(post).build();
            likePostRepository.save(postLike);
            post.upLike();
            return 1;
        }
    }

    @Transactional
    public int scrapPost(Member member, Long postId){
        Post post = postRepository.findByIdWithLock(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(scrapRepository.existsByMemberAndPost(member,post)){
            Scrap scrap = scrapRepository.findByMemberAndPost(member,post).orElseThrow(()->new MyException(MyErrorCode.USER_OR_POST_NOT_FOUND));
            scrapRepository.delete(scrap);
            post.downScrap();
            return -1;
        }
        else{
            Scrap scrap = Scrap.builder().member(member).post(post).build();
            scrapRepository.save(scrap);
            post.upScrap();
            return 1;
        }
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getPostByMember(Member member, String sort, int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8);
        if(sort==null||sort.equals("date")) {
            return postRepository.findAllByMemberOrderByIdDesc(member,pageable)
                    .stream()
                    .map(this::getPostListResponseDto)
                    .collect(Collectors.toList());
        }
        else if(sort.equals("like")){
            return postRepository.findAllByMemberOrderByGoodDescIdDesc(member,pageable)
                    .stream()
                    .map(this::getPostListResponseDto)
                    .collect(Collectors.toList());
        }
        else if(sort.equals("scrap")){
            return postRepository.findAllByMemberOrderByScrapDescIdDesc(member,pageable)
                    .stream()
                    .map(this::getPostListResponseDto)
                    .collect(Collectors.toList());
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

    @Transactional(readOnly = true)
    public ListResponseDto getScrapsByMember(Member member, String sort,int page){
        List<PostListResponseDto> scraps = scrapRepository.findAllByMember(member).stream()
                .map(scrap -> {
                    Post post = scrap.getPost();
                    return getPostListResponseDto(post);
                })
                .sorted(Comparator.comparing(PostListResponseDto::getId).reversed())
                .collect(Collectors.toList());
        page--;
        int startIndex = 0;
        int endIndex = 0;
        long pages = (long)Math.ceil( (double) scraps.size() /5);
        if(page*5>scraps.size()){
            scraps.clear();
        }
        else{
            startIndex = page*5;
            endIndex  = Math.min((page + 1) * 5, scraps.size());
        }
        return  ListResponseDto.of(pages,scraps.size(),postListSort(scraps,sort).subList(startIndex,endIndex));
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getLikeByMember(Member member, String sort){
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
        if(sort==null||sort.equals("date")){
            return posts;
        }
        else if(sort.equals("like")){
            posts.sort((o1, o2) -> o2.getLike().intValue() - o1.getLike().intValue());
            return posts;
        }
        else if(sort.equals("scrap")){
            posts.sort((o1, o2) -> o2.getScrap().intValue()-o1.getScrap().intValue());
            return posts;
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
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
        return PostListResponseDto.of(post,writer);
    }

    @Transactional(readOnly = true)
    public ListResponseDto searchPost(String query,String sort,int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8);
        if (sort == null||sort.equals("date")) {
            Page<Post> dto = postRepository.searchByKeyword(query,pageable);
            return ListResponseDto.of(dto.getTotalPages(),dto.getTotalElements(),dto.stream().map(this::getPostListResponseDto).collect(Collectors.toList()));
        } else if (sort.equals("like")) {
            Page<Post> dto = postRepository.searchByKeywordOrderByLikes(query,pageable);
            return ListResponseDto.of(dto.getTotalPages(), dto.getTotalElements(), dto.stream().map(this::getPostListResponseDto).collect(Collectors.toList()));
        } else if (sort.equals("scrap")) {
            Page<Post> dto = postRepository.searchByKeywordOrderByScraps(query,pageable);
            return ListResponseDto.of(dto.getTotalPages(), dto.getTotalElements(),dto.stream().map(this::getPostListResponseDto).collect(Collectors.toList()));
        } else {
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "topPost", key = "#category != null ? #category : 'default'",cacheManager = "cacheManager")
    public List<PostListResponseDto> getTop(String category){
        return postRepository.findTop12(category).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "randomPost", cacheManager = "cacheManager")
    public List<PostListResponseDto> getRandomTop(){
        return postRepository.findRandomTop().stream().map(this::getPostListResponseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ListResponseDto searchInScrap(Member member, String query,int page, String sort){
        List<PostListResponseDto> scraps = scrapRepository.searchScrap(member,query).stream()
                .map(scrap -> {
                    Post post = scrap.getPost();
                    return getPostListResponseDto(post);
                })
                .sorted(Comparator.comparing(PostListResponseDto::getId).reversed())
                .collect(Collectors.toList());

        page--;
        int startIndex = 0;
        int endIndex = 0;
        long pages = (long)Math.ceil( (double) scraps.size() /5);
        if(page*5>scraps.size()){
            scraps.clear();
        }
        else{
            startIndex = page*5;
            endIndex  = Math.min((page + 1) * 5, scraps.size());
        }
        return  ListResponseDto.of(pages,scraps.size(),postListSort(scraps,sort).subList(startIndex,endIndex));
    }


    @Transactional(readOnly = true)
    public List<PostListResponseDto> getPostForInf(Long lastPostId,String category){
        Pageable pageable = PageRequest.of(0,8);
        if(lastPostId==null&&category==null){
            return postRepository.findAllByOrderByIdDesc(pageable).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
        }
        else if(lastPostId == null){
            return postRepository.findAllByCategoryOrderByIdDesc(category,pageable).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
        }
        else if(category!=null){
           return postRepository.findByCategoryAndIdLessThanOrderByIdDesc(category,lastPostId,pageable).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
        }
        else {
            return postRepository.findAllByIdLessThanOrderByIdDesc(lastPostId, pageable).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
        }
    }



}
