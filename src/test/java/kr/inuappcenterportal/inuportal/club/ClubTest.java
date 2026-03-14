package kr.inuappcenterportal.inuportal.club;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.club.dto.ClubRecruitingRequestDto;
import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import kr.inuappcenterportal.inuportal.domain.club.repository.ClubRepository;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import kr.inuappcenterportal.inuportal.domain.image.service.ImageService;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import com.google.firebase.messaging.FirebaseMessaging;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class ClubTest {
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
    @MockBean
    FirebaseMessaging firebaseMessaging;

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    ClubRepository clubRepository;
    @Autowired
    MemberService memberService;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    MockMvc mockMvc;


    @Test
    @DisplayName("동아리 모집 공고를 등록합니다.")
    public void addRecruitTest() throws Exception {
        Club club = Club.builder().name("testClub").build();
        clubRepository.save(club);
        ClubRecruitingRequestDto requestDto = new ClubRecruitingRequestDto("모집 공고",true);
        Member member = saveAdminMember("201900000");
        List<MultipartFile> images = createDummyImages();
        MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart("/api/clubs/"+club.getId());
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        String body = objectMapper.writeValueAsString(requestDto);
        MockPart jsonPart = new MockPart("clubRecruitingRequestDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("동아리 모집공고 등록 성공"))
                .andExpect(jsonPath("$.data").value(club.getId()))
                .andDo(print());
    }

    @Test
    @DisplayName("동아리 모집 공고에 실패합니다. - 일반 권한")
    public void addRecruitFailTest() throws Exception {
        Club club = Club.builder().name("testClub").build();
        clubRepository.save(club);
        ClubRecruitingRequestDto requestDto = new ClubRecruitingRequestDto("모집 공고",true);
        Member member = saveMember("201900000");
        List<MultipartFile> images = createDummyImages();
        MockMultipartHttpServletRequestBuilder multipartRequest = MockMvcRequestBuilders.multipart("/api/clubs/"+club.getId());
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        String body = objectMapper.writeValueAsString(requestDto);
        MockPart jsonPart = new MockPart("clubRecruitingRequestDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("접근 권한이 없는 사용자입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("동아리 모집 공고를 수정합니다.")
    public void updateRecruitTest() throws Exception {
        Club club = Club.builder().name("testClub").build();
        clubRepository.save(club);
        ClubRecruitingRequestDto requestDto = new ClubRecruitingRequestDto("모집 공고",true);
        Member member = saveAdminMember("201900000");
        List<MultipartFile> images = createDummyImages();
        MockMultipartHttpServletRequestBuilder multipartRequest = (MockMultipartHttpServletRequestBuilder) MockMvcRequestBuilders.multipart("/api/clubs/" +club.getId())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        String body = objectMapper.writeValueAsString(requestDto);
        MockPart jsonPart = new MockPart("clubRecruitingRequestDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("동아리 모집공고 수정 성공"))
                .andExpect(jsonPath("$.data").value(club.getId()))
                .andDo(print());
    }

    @Test
    @DisplayName("동아리 모집 공고를 수정에 실패합니다. - 일반 권한")
    public void updateRecruitFailTest() throws Exception {
        Club club = Club.builder().name("testClub").build();
        clubRepository.save(club);
        ClubRecruitingRequestDto requestDto = new ClubRecruitingRequestDto("모집 공고",true);
        Member member = saveMember("201900000");
        List<MultipartFile> images = createDummyImages();
        MockMultipartHttpServletRequestBuilder multipartRequest = (MockMultipartHttpServletRequestBuilder) MockMvcRequestBuilders.multipart("/api/clubs/" +club.getId())
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
        for(MultipartFile multipartFile : images){
            multipartRequest.file((MockMultipartFile) multipartFile);
        }
        String body = objectMapper.writeValueAsString(requestDto);
        MockPart jsonPart = new MockPart("clubRecruitingRequestDto", body.getBytes(StandardCharsets.UTF_8));
        jsonPart.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        multipartRequest.part(jsonPart);
        TokenDto tokenDto = memberService.login(member);
        multipartRequest.header("Auth", tokenDto.getAccessToken())
                .with(csrf())
                .contentType(MediaType.MULTIPART_FORM_DATA);
        mockMvc.perform(multipartRequest)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.msg").value("접근 권한이 없는 사용자입니다."))
                .andDo(print());
    }

    @Test
    @DisplayName("동아리 모집 공고를 조회합니다.")
    public void getRecruitTest() throws Exception {
        Club club = Club.builder().name("testClub").build();
        clubRepository.save(club);
        club.recruiting("모집 공고",3L,true);
        mockMvc.perform(get("/api/clubs/"+club.getId()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("동아리 모집공고 가져오기 성공"))
                .andExpect(jsonPath("$.data.recruit").value("모집 공고"))
                .andExpect(jsonPath("$.data.imageCount").value(3))
                .andDo(print());
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
}
