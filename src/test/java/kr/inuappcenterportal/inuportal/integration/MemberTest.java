package kr.inuappcenterportal.inuportal.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.CafeteriaService;
import kr.inuappcenterportal.inuportal.domain.member.dto.LoginDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberUpdateNicknameDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.notice.service.NoticeService;
import kr.inuappcenterportal.inuportal.domain.schedule.service.ScheduleService;
import kr.inuappcenterportal.inuportal.domain.weather.service.WeatherService;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

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
public class MemberTest {

    @MockBean
    WeatherService weatherService;
    @MockBean
    ScheduleService scheduleService;
    @MockBean
    NoticeService noticeService;
    @MockBean
    CafeteriaService cafeteriaService;
    @MockBean
    SchoolLoginRepository schoolLoginRepository;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    MemberRepository memberRepository;
    @Autowired
    MemberService memberService;

    @Test
    @DisplayName("로그인 성공 테스트 - 회원가입이 되어있지 않은 회원")
    public void nonMemberLoginTest() throws Exception {
        String studentId= "20241234";
        String password = "12345";
        String body = makeLoginBody(studentId,password);
        when(schoolLoginRepository.loginCheck(studentId,password)).thenReturn(true);
        mockMvc.perform(post("/api/members/login").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("로그인 성공, 토근이 발급되었습니다."))
                .andDo(print());
        verify(schoolLoginRepository,times(1)).loginCheck(studentId,password);
    }

    @Test
    @DisplayName("로그인 성공 테스트 - 회원가입이 되어있는 회원")
    public void memberLoginTest() throws Exception {
        String studentId= "20241234";
        String password = "12345";
        saveMember(studentId);
        String body = makeLoginBody(studentId,password);
        when(schoolLoginRepository.loginCheck(studentId,password)).thenReturn(true);
        mockMvc.perform(post("/api/members/login").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("로그인 성공, 토근이 발급되었습니다."))
                .andDo(print());
        verify(schoolLoginRepository,times(1)).loginCheck(studentId,password);
    }

    @Test
    @DisplayName("로그인 실패 테스트")
    public void memberLoginFailTest() throws Exception {
        String studentId= "20241234";
        String password = "12345";
        saveMember(studentId);
        String body = makeLoginBody(studentId,password);
        when(schoolLoginRepository.loginCheck(studentId,password)).thenReturn(false);
        mockMvc.perform(post("/api/members/login").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("학번 또는 비밀번호가 틀립니다."))
                .andDo(print());
        verify(schoolLoginRepository,times(1)).loginCheck(studentId,password);
    }

    @Test
    @DisplayName("토큰 재발급 성공 테스트")
    public void refreshTokenTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        TokenDto tokenDto = memberService.login(member);

        mockMvc.perform(post("/api/members/refresh").header("refresh",tokenDto.getRefreshToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("토큰 재발급 성공"))
                .andDo(print());
    }

    @Test
    @DisplayName("회원 탈퇴 테스트")
    public void deleteMemberTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        TokenDto tokenDto = memberService.login(member);

        mockMvc.perform(delete("/api/members").header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("회원삭제성공"))
                .andDo(print());

        assertNull(memberRepository.findByStudentId(studentId).orElse(null));
    }

    @Test
    @DisplayName("회원 정보 가져오기 테스트")
    public void getMemberTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        TokenDto tokenDto = memberService.login(member);

        mockMvc.perform(get("/api/members").header("Auth",tokenDto.getAccessToken()).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("회원 가져오기 성공"))
                .andExpect(jsonPath("$.data.id").value(member.getId()))
                .andExpect(jsonPath("$.data.nickname").value(studentId))
                .andExpect(jsonPath("$.data.fireId").value(1L))
                .andDo(print());
    }

    @Test
    @DisplayName("회원 정보 수정 성공 테스트 - 닉네임, 횃불이 둘 다 변경")
    public void updateMemberTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        TokenDto tokenDto = memberService.login(member);
        String updateNickname = "testNick";
        Long updateFIre = 3L;
        String body = createUpdateMemberDto(updateNickname,updateFIre);

        mockMvc.perform(put("/api/members").header("Auth",tokenDto.getAccessToken()).content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("회원 닉네임/횃불이 이미지 변경 성공"))
                .andExpect(jsonPath("$.data").value(member.getId()))
                .andDo(print());

        Member updatedMember = memberRepository.findById(member.getId()).orElse(null);
        assertAll(
                ()->assertEquals(updatedMember.getNickname(),updateNickname),
                ()->assertEquals(updatedMember.getFireId(),updateFIre)
        );
    }

    @Test
    @DisplayName("회원 정보 수정 성공 테스트 - 닉네임만 변경")
    public void updateMemberNicknameTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        TokenDto tokenDto = memberService.login(member);
        String updateNickname = "testNick";
        String body = createUpdateMemberDto(updateNickname,null);

        mockMvc.perform(put("/api/members").header("Auth",tokenDto.getAccessToken()).content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("회원 닉네임/횃불이 이미지 변경 성공"))
                .andExpect(jsonPath("$.data").value(member.getId()))
                .andDo(print());

        Member updatedMember = memberRepository.findById(member.getId()).orElse(null);
        assertAll(
                ()->assertEquals(updatedMember.getNickname(),updateNickname),
                ()->assertEquals(updatedMember.getFireId(),1L)
        );
    }

    @Test
    @DisplayName("회원 정보 수정 성공 테스트 - 횃불이만 변경")
    public void updateMemberFireTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        TokenDto tokenDto = memberService.login(member);
        Long updateFire = 3L;
        String body = createUpdateMemberDto(null,updateFire);

        mockMvc.perform(put("/api/members").header("Auth",tokenDto.getAccessToken()).content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("회원 닉네임/횃불이 이미지 변경 성공"))
                .andExpect(jsonPath("$.data").value(member.getId()))
                .andDo(print());

        Member updatedMember = memberRepository.findById(member.getId()).orElse(null);
        assertAll(
                ()->assertEquals(updatedMember.getNickname(),studentId),
                ()->assertEquals(updatedMember.getFireId(),updateFire)
        );
    }

    @Test
    @DisplayName("회원 정보 수정 실패 테스트 - 중복 닉네임")
    public void updateMemberDuplicatedNicknameTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        saveMember("duplicatedNickname");
        TokenDto tokenDto = memberService.login(member);
        String updateNickname = "duplicatedNickname";
        Long updateFIre = 3L;
        String body = createUpdateMemberDto(updateNickname,updateFIre);

        mockMvc.perform(put("/api/members").header("Auth",tokenDto.getAccessToken()).content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("동일한 닉네임이 존재합니다."))
                .andExpect(jsonPath("$.data").value(-1))
                .andDo(print());
    }

    @Test
    @DisplayName("회원 정보 수정 실패 테스트 - 공백 닉네임")
    public void updateMemberBlankNicknameTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        TokenDto tokenDto = memberService.login(member);
        String updateNickname = "       ";
        Long updateFIre = 3L;
        String body = createUpdateMemberDto(updateNickname,updateFIre);

        mockMvc.perform(put("/api/members").header("Auth",tokenDto.getAccessToken()).content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("닉네임이 빈칸 혹은 공백입니다."))
                .andExpect(jsonPath("$.data").value(-1))
                .andDo(print());
    }

    @Test
    @DisplayName("회원 정보 수정 실패 테스트 - 빈값")
    public void updateMemberNullTest() throws Exception {
        String studentId= "20241234";
        Member member = saveMember(studentId);
        TokenDto tokenDto = memberService.login(member);
        String body = createUpdateMemberDto(null,null);

        mockMvc.perform(put("/api/members").header("Auth",tokenDto.getAccessToken()).content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.msg").value("닉네임, 횃불이 아이디 모두 공백입니다."))
                .andExpect(jsonPath("$.data").value(-1))
                .andDo(print());
    }

    private Member saveMember(String studentId){
        return memberRepository.save(Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build());
    }

    private String makeLoginBody(String studentId, String password) throws JsonProcessingException {
        LoginDto loginDto = LoginDto.builder().studentId(studentId).password(password).build();
        return objectMapper.writeValueAsString(loginDto);
    }

    private String createUpdateMemberDto(String nickname, Long fireId) throws JsonProcessingException {
        MemberUpdateNicknameDto memberUpdateNicknameDto = MemberUpdateNicknameDto.builder().nickname(nickname).fireId(fireId).build();
        return objectMapper.writeValueAsString(memberUpdateNicknameDto);
    }


}
