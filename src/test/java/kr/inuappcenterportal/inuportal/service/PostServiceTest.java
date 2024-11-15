package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.PostDto;
import kr.inuappcenterportal.inuportal.repository.CategoryRepository;
import kr.inuappcenterportal.inuportal.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.when;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private RedisService redisService;
    @Mock
    private CategoryRepository categoryRepository;



    @Test
    @DisplayName("게시글 저장 테스트")
    public void saveOnlyPostTest() throws Exception{
        Member member = Member.builder().nickname("testMember").studentId("201900000").roles(Collections.singletonList("ROLE_USER")).build();
        PostDto postDto = PostDto.builder().title("title").content("content").anonymous(true).category("수강신청").build();
        when(categoryRepository.existsByCategory(any(String.class))).thenReturn(true);
        postService.saveOnlyPost(member,postDto);

    }

    /*@Test
    @DisplayName("게시글 도배 테스트")
    public void postAttackTest() throws Exception{
        Member member = Member.builder().nickname("testMember").studentId("201900000").roles(Collections.singletonList("ROLE_USER")).build();
        PostDto postDto = PostDto.builder().title("title").content("content").anonymous(true).category("수강신청").build();
        doThrow(new MyException(MyErrorCode.BLOCK_MANY_SAME_POST_REPLY)).when(redisService).blockRepeat(any(String.class));
        *//*MyException myException = postService.saveOnlyPost(member,postDto);*//*
        Assertions.assertThrows(MyException.class, ()->postService.saveOnlyPost(member,postDto));
    }*/


}
