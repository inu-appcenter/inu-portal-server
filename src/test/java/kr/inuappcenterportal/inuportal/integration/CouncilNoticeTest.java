package kr.inuappcenterportal.inuportal.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.councilNotice.dto.CouncilNoticeRequestDto;
import kr.inuappcenterportal.inuportal.domain.councilNotice.model.CouncilNotice;
import kr.inuappcenterportal.inuportal.domain.councilNotice.repostiory.CouncilRepository;
import kr.inuappcenterportal.inuportal.domain.councilNotice.service.CouncilNoticeService;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.BDDMockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class CouncilNoticeTest {
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
    CouncilNoticeService councilNoticeService;
    @Autowired
    CouncilRepository councilRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MemberService memberService;

    @Test
    @DisplayName("총학생회 공지사항 작성 테스트")
    public void saveCouncilNoticeTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목","본문");
        String body = objectMapper.writeValueAsString(councilNoticeRequestDto);
        Member member = saveAdminMember("20241234");
        TokenDto tokenDto = memberService.login(member);
        mockMvc.perform(post("/api/councilNotices").content(body).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.msg").value("총학생회 공지사항 등록 성공"))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 공지사항 작성 실패 테스트 - 관리자가 아닌 계정")
    public void saveCouncilNoticeFailTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목","본문");
        String body = objectMapper.writeValueAsString(councilNoticeRequestDto);
        Member member = saveMember("20241234");
        TokenDto tokenDto = memberService.login(member);
        mockMvc.perform(post("/api/councilNotices").content(body).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("접근 권한이 없는 사용자입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 공지사항 이미지 등록 테스트")
    public void saveCouncilNoticeImageTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목","본문");
        Long councilNoticeId =councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);
        List<MultipartFile> images = createDummyImages();
        Member member = saveAdminMember("20241234");
        MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart("/api/councilNotices/" + councilNoticeId + "/images");
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.msg").value("총학생회 공지사항 이미지 등록 성공"))
                .andExpect(jsonPath("$.data").value(councilNoticeId))
                .andDo(print());
        CouncilNotice councilNotice = councilRepository.findById(councilNoticeId).orElse(null);
        assertEquals(councilNotice.getImageCount(),images.size());
    }

    @Test
    @DisplayName("총학생회 공지사항 이미지 등록 실패 테스트 - 존재하지 않는 공지사항")
    public void saveCouncilNoticeImageFailTest() throws Exception {
        List<MultipartFile> images = createDummyImages();
        Member member = saveAdminMember("20241234");
        MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart("/api/councilNotices/" + -1 + "/images");
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.msg").value("존재하지 않는 총학생회 공지사항입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 공지사항 수정 테스트")
    public void updateCouncilNoticeTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목","본문");
        Long councilNoticeId = councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);
        Member member = saveAdminMember("20241234");
        TokenDto tokenDto = memberService.login(member);
        CouncilNoticeRequestDto councilNoticeUpdateDto = createCouncilNoticeRequestDto("수정된 제목","수정된 본문");
        String body = objectMapper.writeValueAsString(councilNoticeUpdateDto);
        mockMvc.perform(put("/api/councilNotices/"+councilNoticeId).content(body).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총삭생회 공지사항 수정 성공"))
                .andExpect(jsonPath("$.data").value(councilNoticeId))
                .andDo(print());
        CouncilNotice councilNotice = councilRepository.findById(councilNoticeId).orElse(null);
        assertAll(
                ()->assertEquals(councilNotice.getTitle(),"수정된 제목"),
                ()->assertEquals(councilNotice.getContent(),"수정된 본문")
        );
    }

    @Test
    @DisplayName("총학생회 공지사항 이미지 수정 테스트")
    public void updateCouncilNoticeImageTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목","본문");
        Long councilNoticeId = councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);
        Member member = saveAdminMember("20241234");
        TokenDto tokenDto = memberService.login(member);
        List<MultipartFile> images = createDummyImages();
        councilNoticeService.saveCouncilNoticeImage(councilNoticeId,images);
        images = createDummyImages2();
        MockMultipartHttpServletRequestBuilder multipartRequest = (MockMultipartHttpServletRequestBuilder) MockMvcRequestBuilders.multipart("/api/councilNotices/" + councilNoticeId + "/images")
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 공지사항 이미지 수정 성공"))
                .andExpect(jsonPath("$.data").value(councilNoticeId))
                .andDo(print());
        CouncilNotice councilNotice = councilRepository.findById(councilNoticeId).orElse(null);
        assertEquals(councilNotice.getImageCount(),images.size());
    }

    @Test
    @DisplayName("총학생회 공지사항 삭제 테스트")
    public void deleteCouncilNoticeTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목","본문");
        Long councilNoticeId = councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);
        Member member = saveAdminMember("20241234");
        TokenDto tokenDto = memberService.login(member);
        mockMvc.perform(delete("/api/councilNotices/"+councilNoticeId).header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 공지사항 삭제 성공"))
                .andExpect(jsonPath("$.data").value(councilNoticeId))
                .andDo(print());

        assertNull(councilRepository.findById(councilNoticeId).orElse(null));
    }

    @Test
    @DisplayName("총학생회 공지사항 가져오기 테스트")
    public void getCouncilNoticeTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목","본문");
        Long councilNoticeId = councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);
        mockMvc.perform(get("/api/councilNotices/"+councilNoticeId).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 공지사항 가져오기 성공"))
                .andExpect(jsonPath("$.data.id").value(councilNoticeId))
                .andExpect(jsonPath("$.data.title").value("제목"))
                .andExpect(jsonPath("$.data.content").value("본문"))
                .andExpect(jsonPath("$.data.imageCount").value(0))
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 공지사항 이미지 가져오기 테스트")
    public void getCouncilNoticeImageTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목","본문");
        Long councilNoticeId =councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);
        List<MultipartFile> images = createDummyImages();
        councilNoticeService.saveCouncilNoticeImage(councilNoticeId,images);
        mockMvc.perform(get("/api/councilNotices/"+councilNoticeId+"/images/1").with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("총학생회 공지사항 리스트 가져오기")
    public void getCouncilNoticeListTest() throws Exception {
        CouncilNoticeRequestDto councilNoticeRequestDto = createCouncilNoticeRequestDto("제목1","본문1");
        councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);
        councilNoticeRequestDto = createCouncilNoticeRequestDto("제목2","본문2");
        councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);
        councilNoticeRequestDto = createCouncilNoticeRequestDto("제목3","본문3");
        councilNoticeService.saveCouncilNotice(councilNoticeRequestDto);

        mockMvc.perform(get("/api/councilNotices").with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("총학생회 공지사항 리스트 가져오기 성공"))
                .andExpect(jsonPath("$.data.contents[0].title").value("제목3"))
                .andExpect(jsonPath("$.data.contents[1].title").value("제목2"))
                .andExpect(jsonPath("$.data.contents[2].title").value("제목1"))

                .andDo(print());
    }

    private CouncilNoticeRequestDto createCouncilNoticeRequestDto(String title, String content){
        return CouncilNoticeRequestDto.builder().title(title).content(content).build();
    }

    private Member saveAdminMember(String studentId){
        return memberRepository.save(Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_ADMIN")).build());
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

    private List<MultipartFile> createDummyImages2(){
        MockMultipartFile mockMultipartFile1 = new MockMultipartFile("images","image1.jpg","image/jpeg","dummy".getBytes());
        MockMultipartFile mockMultipartFile2= new MockMultipartFile("images","image2.jpg","image/jpeg","dummy".getBytes());
        return Arrays.asList(mockMultipartFile1,mockMultipartFile2);
    }
}
