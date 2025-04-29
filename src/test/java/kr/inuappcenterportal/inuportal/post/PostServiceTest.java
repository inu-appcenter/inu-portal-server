package kr.inuappcenterportal.inuportal.post;

import kr.inuappcenterportal.inuportal.domain.category.repository.CategoryRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostDto;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.post.service.PostService;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {
    @InjectMocks
    PostService postService;
    @Mock
    PostRepository postRepository;
    @Mock
    RedisService redisService;
    @Mock
    ImageService imageService;
    @Mock
    CategoryRepository categoryRepository;

    @Test
    @DisplayName("게시글을 저장합니다.")
    public void savePostTest() throws NoSuchAlgorithmException, IOException {
        //given
        Member member = createMember("201900000");
        List<MultipartFile> images = createDummyImages();
        PostDto postDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        Post post = Post.builder().title(postDto.getTitle()).category(postDto.getCategory()).content(postDto.getContent()).member(member).anonymous(postDto.getAnonymous()).build();
        ReflectionTestUtils.setField(post,"id",1L);
        when(postRepository.save(any())).thenReturn(post);
        when(categoryRepository.existsByCategory("수강신청")).thenReturn(true);

        //when
        Long id = postService.savePost(member,postDto,images);

        //then
        assertEquals(id,1L);
        verify(postRepository,times(1)).save(any());
        verify(categoryRepository,times(1)).existsByCategory("수강신청");
    }

    @Test
    @DisplayName("게시글을 저장에 실패합니다. - 존재하지 않는 카테고리")
    public void savePostFailTest(){
        //given
        Member member = createMember("201900000");
        List<MultipartFile> images = createDummyImages();
        PostDto postDto = PostDto.builder().title("title").content("content").category("축구").anonymous(true).build();
        Post post = Post.builder().title(postDto.getTitle()).category(postDto.getCategory()).content(postDto.getContent()).member(member).anonymous(postDto.getAnonymous()).build();
        ReflectionTestUtils.setField(post,"id",1L);
        when(categoryRepository.existsByCategory("축구")).thenReturn(false);

        //when
        MyException exception = assertThrows(MyException.class,()->postService.savePost(member,postDto,images));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"존재하지 않는 카테고리입니다.");
        verify(categoryRepository,times(1)).existsByCategory("축구");
    }

    @Test
    @DisplayName("게시글을 수정합니다.")
    public void updatePostTest() throws IOException {
        //given
        Member member = createMember("201900000");
        PostDto postDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        Post post = Post.builder().title("title1").category("학사").content("content1").member(member).anonymous(false).build();
        ReflectionTestUtils.setField(post,"id",1L);
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));
        when(categoryRepository.existsByCategory("수강신청")).thenReturn(true);

        //when
        postService.updatePost(member.getId(),1L,postDto,null);

        //then
        verify(postRepository,times(1)).findByIdAndIsDeletedFalse(1L);
        verify(categoryRepository,times(1)).existsByCategory("수강신청");
    }

    @Test
    @DisplayName("게시글 수정에 실패합니다 - 게시글 주인 외 사람 수정 시도")
    public void updateFailTest() throws IOException {
        //given
        Member member = createMember("201900000");
        Member member2 = createMember("20150000");
        ReflectionTestUtils.setField(member2,"id",2L);
        PostDto postDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        Post post = Post.builder().title("title1").category("학사").content("content1").member(member).anonymous(false).build();
        ReflectionTestUtils.setField(post,"id",1L);
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));
        when(categoryRepository.existsByCategory("수강신청")).thenReturn(true);

        //when
        MyException exception = assertThrows(MyException.class,()->postService.updatePost(member2.getId(),post.getId(),postDto,null));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"이 게시글의 수정/삭제에 대한 권한이 없습니다.");
        verify(postRepository,times(1)).findByIdAndIsDeletedFalse(1L);
        verify(categoryRepository,times(1)).existsByCategory("수강신청");
    }
    @Test
    @DisplayName("게시글을 삭제합니다")
    public void deletePostTest(){
        //given
        Member member = createMember("201900000");
        Post post = Post.builder().title("title1").category("학사").content("content1").member(member).anonymous(false).build();
        ReflectionTestUtils.setField(post,"id",1L);
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));

        //when
        postService.delete(member,post.getId());

        //then
        verify(postRepository,times(1)).findByIdAndIsDeletedFalse(1L);
    }

    @Test
    @DisplayName("게시글 삭제에 실패합니다 - 다른 사용자")
    public void deletePostFailTest(){
        //given
        Member member = createMember("201900000");
        Member member1 = createMember("201500000");
        ReflectionTestUtils.setField(member1,"id",2L);
        Post post = Post.builder().title("title1").category("학사").content("content1").member(member).anonymous(false).build();
        ReflectionTestUtils.setField(post,"id",1L);
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));

        //when
        MyException exception = assertThrows(MyException.class,()->postService.delete(member1,post.getId()));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"이 게시글의 수정/삭제에 대한 권한이 없습니다.");
        verify(postRepository,times(1)).findByIdAndIsDeletedFalse(1L);
    }

    @Test
    @DisplayName("게시글을 삭제합니다 - 관리자")
    public void deletePostAdmin(){
        //given
        Member member = createMember("201900000");
        Member adminMember = createMember("201500000");
        ReflectionTestUtils.setField(adminMember,"id",2L);
        ReflectionTestUtils.setField(adminMember,"roles",Collections.singletonList("ROLE_ADMIN"));
        Post post = Post.builder().title("title1").category("학사").content("content1").member(member).anonymous(false).build();
        ReflectionTestUtils.setField(post,"id",1L);
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(post));

        //when
        postService.delete(adminMember,post.getId());

        //then
        verify(postRepository,times(1)).findByIdAndIsDeletedFalse(1L);
    }


    private Member createMember(String studentId){
        Member member = Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build();
        ReflectionTestUtils.setField(member,"id",1L);
        return member;
    }
    private List<MultipartFile> createDummyImages(){
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("image","image1.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile2= new MockMultipartFile("image","image2.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile3 = new MockMultipartFile("image","image3.jpg","image/jpeg","dummy".getBytes());
        return Arrays.asList(mockMultipartFile1,mockMultipartFile2,mockMultipartFile3);
    }
}
