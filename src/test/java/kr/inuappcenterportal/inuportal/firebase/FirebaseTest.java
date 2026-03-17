package kr.inuappcenterportal.inuportal.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessaging;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.req.TokenRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmToken;
import kr.inuappcenterportal.inuportal.domain.firebase.repository.FcmTokenRepository;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncExecutor;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.image.service.ImageService;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class FirebaseTest {

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
    @DisplayName("save token for anonymous member")
    void tokenSaveTest() throws Exception {
        String token = "token1";
        TokenRequestDto tokenRequestDto = TokenRequestDto.builder().token(token).build();

        mockMvc.perform(post("/api/tokens")
                        .with(csrf())
                        .content(objectMapper.writeValueAsString(tokenRequestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1))
                .andDo(print());

        assertTrue(fcmTokenRepository.existsByToken(token));
    }

    @Test
    @DisplayName("save token for logged-in member")
    void tokenSaveLoginTest() throws Exception {
        Member member = saveMember("201900000");
        TokenDto tokenDto = memberService.login(member);
        String token = "token2";
        TokenRequestDto tokenRequestDto = TokenRequestDto.builder().token(token).build();

        mockMvc.perform(post("/api/tokens")
                        .with(csrf())
                        .header("Auth", tokenDto.getAccessToken())
                        .content(objectMapper.writeValueAsString(tokenRequestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1))
                .andDo(print());

        FcmToken fcmToken = fcmTokenRepository.findByToken(token).orElseThrow();
        assertAll(
                () -> assertEquals(token, fcmToken.getToken()),
                () -> assertEquals(member.getId(), fcmToken.getMemberId())
        );
    }

    @Test
    @DisplayName("delete token only unlinks member")
    void tokenDeleteTest() throws Exception {
        Member member = saveMember("201900000");
        TokenDto tokenDto = memberService.login(member);
        String token = "token3";

        fcmTokenRepository.save(FcmToken.builder().memberId(member.getId()).token(token).build());
        TokenRequestDto tokenRequestDto = TokenRequestDto.builder().token(token).build();

        mockMvc.perform(delete("/api/tokens")
                        .with(csrf())
                        .header("Auth", tokenDto.getAccessToken())
                        .content(objectMapper.writeValueAsString(tokenRequestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1))
                .andDo(print());

        FcmToken fcmToken = fcmTokenRepository.findByToken(token).orElseThrow();
        assertNull(fcmToken.getMemberId());
    }

    @Test
    @DisplayName("anonymous save does not unlink existing token owner")
    void anonymousSaveDoesNotUnlinkExistingMemberToken() throws Exception {
        Member member = saveMember("201900001");
        String token = "token4";

        fcmTokenRepository.save(FcmToken.builder()
                .memberId(member.getId())
                .token(token)
                .deviceType("android")
                .build());

        String requestBody = """
                {
                  "token": "token4",
                  "deviceType": "ios"
                }
                """;

        mockMvc.perform(post("/api/tokens")
                        .with(csrf())
                        .content(requestBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        FcmToken fcmToken = fcmTokenRepository.findByToken(token).orElseThrow();
        assertAll(
                () -> assertEquals(member.getId(), fcmToken.getMemberId()),
                () -> assertEquals("ios", fcmToken.getDeviceType())
        );
    }

    @Test
    @DisplayName("delete token rejects another member")
    void deleteOtherMembersTokenReturnsNotFound() throws Exception {
        Member owner = saveMember("201900002");
        Member anotherMember = saveMember("201900003");
        TokenDto tokenDto = memberService.login(anotherMember);
        String token = "token5";

        fcmTokenRepository.save(FcmToken.builder().memberId(owner.getId()).token(token).build());
        TokenRequestDto tokenRequestDto = TokenRequestDto.builder().token(token).build();

        mockMvc.perform(delete("/api/tokens")
                        .with(csrf())
                        .header("Auth", tokenDto.getAccessToken())
                        .content(objectMapper.writeValueAsString(tokenRequestDto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        FcmToken fcmToken = fcmTokenRepository.findByToken(token).orElseThrow();
        assertEquals(owner.getId(), fcmToken.getMemberId());
    }

    private Member saveMember(String studentId) {
        return memberRepository.save(Member.builder()
                .studentId(studentId)
                .roles(Collections.singletonList("ROLE_USER"))
                .build());
    }
}
