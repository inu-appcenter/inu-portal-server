package kr.inuappcenterportal.inuportal.domain.post.service;

import kr.inuappcenterportal.inuportal.domain.category.repository.CategoryRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostDto;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.postLike.model.PostLike;
import kr.inuappcenterportal.inuportal.domain.postLike.repository.LikePostRepository;
import kr.inuappcenterportal.inuportal.domain.reply.service.ReplyService;
import kr.inuappcenterportal.inuportal.domain.report.repository.ReportRepository;
import kr.inuappcenterportal.inuportal.domain.scrap.model.Scrap;
import kr.inuappcenterportal.inuportal.domain.scrap.repository.ScrapRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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
    private final ImageService imageService;
    private final ReportRepository reportRepository;

    @Value("${postImagePath}")
    private String path;


    @Transactional
    public Long saveOnlyPost(Member member, PostDto postSaveDto, List<MultipartFile> images) throws NoSuchAlgorithmException, IOException {
        if(!categoryRepository.existsByCategory(postSaveDto.getCategory())){
            throw new MyException(MyErrorCode.CATEGORY_NOT_FOUND);
        }
        String hash = postSaveDto.getTitle()+postSaveDto.getContent();
        redisService.blockRepeat(hash);
        Post post = Post.builder().title(postSaveDto.getTitle()).content(postSaveDto.getContent()).anonymous(postSaveDto.getAnonymous()).category(postSaveDto.getCategory()).member(member).imageCount(0).build();
        postRepository.save(post);
        if (images != null) {
            post.updateImageCount(images.size());
            imageService.saveImageWithThumbnail(post.getId(),images,path);
        }
        return post.getId();
    }



    public byte[] getPostImage(Long postId, Long imageId){
        return imageService.getImage(postId,imageId,path);
    }

    @Transactional
    public void updateOnlyPost(Long memberId, Long postId, PostDto postDto, List<MultipartFile> images) throws IOException {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(post.getIsDeleted()){
            throw new MyException(MyErrorCode.POST_NOT_FOUND);
        }
        if(!categoryRepository.existsByCategory(postDto.getCategory())){
            throw new MyException(MyErrorCode.CATEGORY_NOT_FOUND);
        }
        if(!post.getMember().getId().equals(memberId)){
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        post.updateOnlyPost(postDto.getTitle(),postDto.getContent(), postDto.getCategory(),postDto.getAnonymous());
        if(images!=null){
            imageService.updateImages(postId,images,path);
            post.updateImageCount(images.size());
        }
    }



    @Transactional
    public void delete(Member member, Long postId) throws IOException {
        Post post = postRepository.findByIdAndIsDeletedFalse(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(!post.getMember().getId().equals(member.getId())&&!member.getRoles().contains("ROLE_ADMIN")){
            throw new MyException(MyErrorCode.HAS_NOT_POST_AUTHORIZATION);
        }
        post.delete();
    }

    @Transactional
    public PostResponseDto getPost(Long postId,Member member,String address){
        Post post = postRepository.findByIdAndIsDeletedFalseWithPostMember(postId).orElseThrow(()->new MyException(MyErrorCode.POST_NOT_FOUND));
        if(member!=null&&reportRepository.existsByPostIdAndMemberId(postId,member.getId())){
            throw new MyException(MyErrorCode.BANNED_POST);
        }
        if(redisService.isFirstConnect(address,postId,"post")){
            redisService.insertAddress(address,postId,"post");
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
            if(post.getMember()!=null&&(member.getId().equals(post.getMember().getId())||member.getRoles().contains("ROLE_ADMIN"))){
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
    public ListResponseDto<PostListResponseDto> getAllPost(String category, String sort, int page, Member member){
        Pageable pageable = PageRequest.of(page>0?--page:page,8,sortData(sort));
        Page<Post> dto;
        List<Long> postIds = null;
        if (category != null && !categoryRepository.existsByCategory(category)) {
            throw new MyException(MyErrorCode.CATEGORY_NOT_FOUND);
        }
        if(member!=null) {
            postIds = reportRepository.findPostIdsByMemberId(member.getId());
            if(postIds.size()==0){
                postIds = null;
            }
        }
        dto = postRepository.findAllByCategoryExcludingPostIds(category,postIds,pageable);
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
        if(post.getIsDeleted()){
            throw new MyException(MyErrorCode.POST_NOT_FOUND);
        }
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
    public List<PostListResponseDto> getPostByMember(Member member, String sort){
        return postRepository.findAllByMemberAndIsDeletedFalse(member,sortData(sort))
                .stream()
                .map(this::getPostListResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ListResponseDto<PostListResponseDto> getScrapsByMember(Member member, String sort,int page){
        List<PostListResponseDto> scraps = scrapRepository.findAllByMember(member, sortFetchJoin(sort)).stream()
                .map(scrap -> {
                    Post post = scrap.getPost();
                    return getPostListResponseDto(post);
                })
                .collect(Collectors.toList());
        return pagingFetchJoin(page,scraps);
    }

    @Transactional(readOnly = true)
    public List<PostListResponseDto> getLikeByMember(Member member, String sort){
        return likePostRepository.findAllByMember(member, sortFetchJoin(sort)).stream()
                .map(like -> {
                    Post post = like.getPost();
                    return getPostListResponseDto(post);
                })
                .collect(Collectors.toList());
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
    public ListResponseDto<PostListResponseDto> searchPost(String query,String sort,int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8);
        if (sort.equals("date")) {
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
    public ListResponseDto<PostListResponseDto> searchInScrap(Member member, String query,int page, String sort){
        List<PostListResponseDto> scraps  = scrapRepository.searchScrap(member,query, sortFetchJoin(sort)).stream()
                .map(scrap -> {
                    Post post = scrap.getPost();
                    return getPostListResponseDto(post);
                })
                .collect(Collectors.toList());
        return pagingFetchJoin(page,scraps);
    }


    @Transactional(readOnly = true)
    public List<PostListResponseDto> getPostForInf(Long lastPostId,String category, Member member){
        Pageable pageable = PageRequest.of(0,8,sortData("date"));
        List<Long> postIds = null;
        if(member!=null){
            postIds = reportRepository.findPostIdsByMemberId(member.getId());
            if(postIds.size()==0){
                postIds = null;
            }
        }
        if(lastPostId==null){
            return postRepository.findAllByCategoryExcludingPostIds(category,postIds,pageable).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
        }
        else if(category!=null){
            return postRepository.findByCategoryAndIdLessThanAndIsDeletedFalse(category,lastPostId,pageable).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
        }
        else {
            return postRepository.findFilteredPosts(category,lastPostId,postIds, pageable).stream().map(this::getPostListResponseDto).collect(Collectors.toList());
        }
    }

    public ListResponseDto<PostListResponseDto> pagingFetchJoin(int page, List<PostListResponseDto> dto){
        int total = dto.size();
        page--;
        int startIndex = 0;
        int endIndex = 0;
        long pages = (long)Math.ceil( (double) dto.size() /5);
        if(page*5>dto.size()){
            dto.clear();
        }
        else{
            startIndex = page*5;
            endIndex  = Math.min((page + 1) * 5, dto.size());
        }
        return  ListResponseDto.of(pages,total,dto.subList(startIndex,endIndex));
    }

    public Sort sortFetchJoin(String sort){
        if(sort.equals("date")){
            return Sort.by(Sort.Direction.DESC, "post.id");
        }
        else if(sort.equals("like")){
            return Sort.by(Sort.Direction.DESC, "post.good","post.id");
        }
        else if(sort.equals("scrap")){
            return Sort.by(Sort.Direction.DESC, "post.scrap","post.id");
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }
    private Sort sortData(String sort){
        if(sort.equals("date")){
            return Sort.by(Sort.Direction.DESC, "id");
        }
        else if(sort.equals("like")){
            return Sort.by(Sort.Direction.DESC, "good","id");
        }
        else if(sort.equals("scrap")){
            return Sort.by(Sort.Direction.DESC, "scrap","id");
        }
        else{
            throw new MyException(MyErrorCode.WRONG_SORT_TYPE);
        }
    }

}
