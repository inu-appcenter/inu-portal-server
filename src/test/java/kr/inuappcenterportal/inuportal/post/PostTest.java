package kr.inuappcenterportal.inuportal.post;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.category.model.Category;
import kr.inuappcenterportal.inuportal.domain.category.repository.CategoryRepository;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.petition.respoitory.PetitionRepository;
import kr.inuappcenterportal.inuportal.domain.petition.service.PetitionService;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostDto;
import kr.inuappcenterportal.inuportal.domain.post.model.Post;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.post.service.PostService;
import kr.inuappcenterportal.inuportal.domain.postLike.model.PostLike;
import kr.inuappcenterportal.inuportal.domain.postLike.repository.LikePostRepository;
import kr.inuappcenterportal.inuportal.domain.report.model.Report;
import kr.inuappcenterportal.inuportal.domain.report.repository.ReportRepository;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.scrap.model.Scrap;
import kr.inuappcenterportal.inuportal.domain.scrap.repository.ScrapRepository;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class PostTest {
    @MockBean
    WeatherService weatherService;
    @MockBean
    ScheduleService scheduleService;
    @MockBean
    NoticeService noticeService;
    @MockBean
    CafeteriaService cafeteriaService;
    @MockBean
    ImageService imageService;
    @MockBean
    SchoolLoginRepository schoolLoginRepository;
    @MockBean
    RedisService redisService;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PostService postService;
    @Autowired
    PostRepository postRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MemberService memberService;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    LikePostRepository likePostRepository;
    @Autowired
    ScrapRepository scrapRepository;
    @Autowired
    ReportRepository reportRepository;


    @Test
    @DisplayName("게시글을 저장합니다.")
    public void savePostTest() throws Exception {
        Member member = saveMember("201900000");
        categoryRepository.save(Category.builder().category("수강신청").build());
        PostDto postDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        List<MultipartFile> images = createDummyImages();
        MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart("/api/posts");
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        String body = objectMapper.writeValueAsString(postDto);
        MockPart jsonPart = new MockPart("postDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.msg").value("게시글 등록 성공"))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 등록에 실패합니다. - 존재하지 않는 카테고리")
    public void savePostFailTest() throws Exception {
        Member member = saveMember("201900000");
        PostDto postDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart("/api/posts");
        String body = objectMapper.writeValueAsString(postDto);
        MockPart jsonPart = new MockPart("postDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msg").value("존재하지 않는 카테고리입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글을 수정합니다.")
    public void updatePostTest() throws Exception {
        Member member = saveMember("201900000");
        categoryRepository.save(Category.builder().category("수강신청").build());
        PostDto postSaveDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        Long postId = postService.savePost(member,postSaveDto,null);
        PostDto postUpdateDto = PostDto.builder().title("updatedTitle").content("updatedContent").category("수강신청").anonymous(false).build();
        MockMultipartHttpServletRequestBuilder multipartRequest = (MockMultipartHttpServletRequestBuilder) MockMvcRequestBuilders.multipart("/api/posts/" +postId)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
        String body = objectMapper.writeValueAsString(postUpdateDto);
        MockPart jsonPart = new MockPart("postDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(postId))
                .andExpect(jsonPath("$.msg").value("게시글 수정 성공"))
                .andDo(print());
        Post post = postRepository.findById(postId).get();
        assertAll(
                ()->assertEquals(post.getTitle(),"updatedTitle"),
                ()->assertEquals(post.getContent(),"updatedContent")
        );
    }

    @Test
    @DisplayName("게시글 수정에 실패합니다. - 권한이 없는 사용자")
    public void updatePostFailTest() throws Exception {
        Member member = saveMember("201900000");
        Member viewMember = saveMember("201901234");
        categoryRepository.save(Category.builder().category("수강신청").build());
        PostDto postSaveDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        Long postId = postService.savePost(member,postSaveDto,null);
        PostDto postUpdateDto = PostDto.builder().title("updatedTitle").content("updatedContent").category("수강신청").anonymous(false).build();
        MockMultipartHttpServletRequestBuilder multipartRequest = (MockMultipartHttpServletRequestBuilder) MockMvcRequestBuilders.multipart("/api/posts/" +postId)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
        String body = objectMapper.writeValueAsString(postUpdateDto);
        MockPart jsonPart = new MockPart("postDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(viewMember);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("이 게시글의 수정/삭제에 대한 권한이 없습니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글을 삭제합니다.")
    public void deletePostTest() throws Exception {
        Member member = saveMember("201900000");
        categoryRepository.save(Category.builder().category("수강신청").build());
        TokenDto tokenDto = memberService.login(member);
        PostDto postSaveDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        Long postId = postService.savePost(member,postSaveDto,null);
        mockMvc.perform(delete("/api/posts/"+postId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("게시글 삭제 성공"))
                .andExpect(jsonPath("$.data").value(postId))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글을 조회합니다. - 비회원, 익명게시글")
    public void getPostTest1() throws Exception {
        Member member = saveMember("201900000");
        categoryRepository.save(Category.builder().category("수강신청").build());
        PostDto postSaveDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(true).build();
        Long postId = postService.savePost(member,postSaveDto,null);
        mockMvc.perform(get("/api/posts/"+postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("게시글 가져오기 성공"))
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.content").value("content"))
                .andExpect(jsonPath("$.data.category").value("수강신청"))
                .andExpect(jsonPath("$.data.writer").value("횃불이"))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.isScraped").value(false))
                .andExpect(jsonPath("$.data.hasAuthority").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글을 조회합니다. - 비회원, 비익명게시글")
    public void getPostTest2() throws Exception {
        Member member = saveMember("201900000");
        categoryRepository.save(Category.builder().category("수강신청").build());
        PostDto postSaveDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(false).build();
        Long postId = postService.savePost(member,postSaveDto,null);
        mockMvc.perform(get("/api/posts/"+postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("게시글 가져오기 성공"))
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.content").value("content"))
                .andExpect(jsonPath("$.data.category").value("수강신청"))
                .andExpect(jsonPath("$.data.writer").value("201900000"))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.isScraped").value(false))
                .andExpect(jsonPath("$.data.hasAuthority").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글을 조회합니다. - 회원, 좋아요, 스크랩한 게시글")
    public void getPostTest3() throws Exception {
        Member member = saveMember("201900000");
        Member viewMember = saveMember("201500000");
        categoryRepository.save(Category.builder().category("수강신청").build());
        PostDto postSaveDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(false).build();
        Long postId = postService.savePost(member,postSaveDto,null);
        Post post = postRepository.findById(postId).get();
        likePostRepository.save(PostLike.builder().post(post).member(viewMember).build());
        scrapRepository.save(Scrap.builder().post(post).member(viewMember).build());
        TokenDto tokenDto = memberService.login(viewMember);
        mockMvc.perform(get("/api/posts/"+postId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("게시글 가져오기 성공"))
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.content").value("content"))
                .andExpect(jsonPath("$.data.category").value("수강신청"))
                .andExpect(jsonPath("$.data.writer").value("201900000"))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.isScraped").value(true))
                .andExpect(jsonPath("$.data.hasAuthority").value(false))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글을 조회합니다. - 본인 게시글")
    public void getPostTest4() throws Exception {
        Member member = saveMember("201900000");
        categoryRepository.save(Category.builder().category("수강신청").build());
        PostDto postSaveDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(false).build();
        Long postId = postService.savePost(member,postSaveDto,null);
        TokenDto tokenDto = memberService.login(member);
        mockMvc.perform(get("/api/posts/"+postId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("게시글 가져오기 성공"))
                .andExpect(jsonPath("$.data.title").value("title"))
                .andExpect(jsonPath("$.data.content").value("content"))
                .andExpect(jsonPath("$.data.category").value("수강신청"))
                .andExpect(jsonPath("$.data.writer").value("201900000"))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.isScraped").value(false))
                .andExpect(jsonPath("$.data.hasAuthority").value(true))
                .andDo(print());
    }

    @Test
    @DisplayName("게시글 조회에 실패합니다 - 차단한 게시글")
    public void getPostFailTest() throws Exception {
        Member member = saveMember("201900000");
        Member viewMember = saveMember("201500000");
        categoryRepository.save(Category.builder().category("수강신청").build());
        PostDto postSaveDto = PostDto.builder().title("title").content("content").category("수강신청").anonymous(false).build();
        Long postId = postService.savePost(member,postSaveDto,null);
        reportRepository.save(Report.builder().postId(postId).memberId(viewMember.getId()).build());
        TokenDto tokenDto = memberService.login(viewMember);
        mockMvc.perform(get("/api/posts/"+postId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msg").value("차단한 게시글입니다."))
                .andDo(print());
    }
    private Member saveMember(String studentId){
        return memberRepository.save(Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build());
    }
    private List<MultipartFile> createDummyImages(){
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("images","image1.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile2= new MockMultipartFile("images","image2.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile3 = new MockMultipartFile("images","image3.jpg","image/jpeg","dummy".getBytes());
        return Arrays.asList(mockMultipartFile1,mockMultipartFile2,mockMultipartFile3);
    }
}
