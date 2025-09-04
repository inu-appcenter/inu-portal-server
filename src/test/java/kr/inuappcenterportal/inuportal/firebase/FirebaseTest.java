package kr.inuappcenterportal.inuportal.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.TokenRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncExecutor;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public class FirebaseTest {
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
    FcmService fcmService;
    @Autowired
    FcmAsyncExecutor fcmAsyncExecutor;
    @Autowired
    FcmTokenRepository fcmTokenRepository;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MemberService memberService;


    @Test
    @DisplayName("토큰을 저장합니다 - 비로그인 회원")
    public void tokenSaveTest() throws Exception {
        String token = "token1";
        TokenRequestDto tokenRequestDto = TokenRequestDto.builder().token(token).build();
        mockMvc.perform(post("/api/tokens").with(csrf()).content(objectMapper.writeValueAsString(tokenRequestDto)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("토큰 등록 성공"))
                .andExpect(jsonPath("$.data").value(1))
                .andDo(print());
        assertTrue(fcmTokenRepository.existsByToken(token));
    }

    @Test
    @DisplayName("토큰을 저장합니다 - 로그인 한 회원")
    public void tokenSaveLoginTest() throws Exception {
        Member member = saveMember("201900000");
        TokenDto tokenDto = memberService.login(member);
        String token = "token2";
        TokenRequestDto tokenRequestDto = TokenRequestDto.builder().token(token).build();
        mockMvc.perform(post("/api/tokens").with(csrf()).header("Auth",tokenDto.getAccessToken()).content(objectMapper.writeValueAsString(tokenRequestDto)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("토큰 등록 성공"))
                .andExpect(jsonPath("$.data").value(1))
                .andDo(print());
        FcmToken fcmToken = fcmTokenRepository.findByToken(token).get();
        assertAll(
                ()->assertEquals(fcmToken.getToken(),token),
                ()->assertEquals(fcmToken.getMemberId(),member.getId())
        );
    }

    @Test
    @DisplayName("토큰을 삭제합니다")
    public void tokenDeleteTest() throws Exception {
        Member member = saveMember("201900000");
        TokenDto tokenDto = memberService.login(member);
        String token = "token3";

        fcmTokenRepository.save(FcmToken.builder().memberId(member.getId()).token(token).build());
        mockMvc.perform(delete("/api/tokens").with(csrf()).header("Auth",tokenDto.getAccessToken()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("토큰에서 회원 정보 삭제 성공"))
                .andExpect(jsonPath("$.data").value(1))
                .andDo(print());
        FcmToken fcmToken = fcmTokenRepository.findByToken(token).get();
        assertNull(fcmToken.getMemberId());
    }

    private Member saveMember(String studentId){
        return memberRepository.save(Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build());
    }
}
