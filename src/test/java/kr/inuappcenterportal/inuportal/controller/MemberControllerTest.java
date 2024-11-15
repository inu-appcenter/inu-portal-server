package kr.inuappcenterportal.inuportal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.inuappcenterportal.inuportal.config.SecurityConfig;
import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.LoginDto;
import kr.inuappcenterportal.inuportal.dto.MemberResponseDto;
import kr.inuappcenterportal.inuportal.dto.MemberUpdateNicknameDto;
import kr.inuappcenterportal.inuportal.dto.TokenDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.service.MemberService;
import kr.inuappcenterportal.inuportal.service.PostService;
import kr.inuappcenterportal.inuportal.service.ReplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(MemberController.class)
@MockBean(JpaMetamodelMappingContext.class)
@Import(SecurityConfig.class)
public class MemberControllerTest {
    @Autowired
    MockMvc mockMvc;
    ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private MemberController memberController;

    @MockBean
    TokenProvider tokenProvider;

    @MockBean
    MemberService memberService;
    @MockBean
    ReplyService replyService;
    @MockBean
    PostService postService;


    @Test
    @DisplayName("로그인 성공 테스트")
    void loginSuccessTest() throws Exception{
        TokenDto tokenDto = TokenDto.of("testToken","testRefreshToken","testExpiredTime","testExpiredTime");
        LoginDto loginDto = LoginDto.builder().studentId("201901591").password("12345").build();
        given(memberService.schoolLogin(any(LoginDto.class))).willReturn(tokenDto);
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/api/members/login").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("로그인 성공, 토근이 발급되었습니다."))
                .andDo(print());
        verify(memberService).schoolLogin(any(LoginDto.class));
    }



    @Test
    @DisplayName("로그인 실패 테스트")
    void loginFailTest() throws Exception{
        LoginDto loginDto = LoginDto.builder().studentId("201901591").password("12345").build();
        when(memberService.schoolLogin(any(LoginDto.class))).thenThrow(new MyException(MyErrorCode.STUDENT_LOGIN_ERROR));
        String body = objectMapper.writeValueAsString(loginDto);
        mockMvc.perform(post("/api/members/login").content(body).with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("학번 또는 비밀번호가 틀립니다."))
                .andExpect(jsonPath("$.data").value(-1))
                .andDo(print());
        verify(memberService).schoolLogin(any(LoginDto.class));
    }

    @Test
    @DisplayName("토큰 재발급 성공 테스트")
    void refreshSuccessTest() throws Exception{
        TokenDto tokenDto = TokenDto.of("testToken","testRefreshToken","testExpiredTime","testExpiredTime");
        when(memberService.refreshToken(any(String.class))).thenReturn(tokenDto);
        mockMvc.perform(post("/api/members/refresh").header("refresh", "testRefreshToken").with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("토큰 재발급 성공"))
                .andDo(print());
        verify(memberService).refreshToken(any(String.class));
    }

    @Test
    @DisplayName("토큰 재발급 실패 테스트")
    void refreshExpiredTokenTest() throws Exception{
        when(memberService.refreshToken(any(String.class))).thenThrow(new MyException(MyErrorCode.EXPIRED_TOKEN));
        mockMvc.perform(post("/api/members/refresh").header("refresh", "testRefreshToken").with(csrf()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.msg").value("만료된 토큰입니다."))
                .andExpect(jsonPath("$.data").value(-1))
                .andDo(print());
        verify(memberService).refreshToken(any(String.class));
    }

    @Test
    @DisplayName("회원정보 가져오기 테스트")
    void getMemberTest() throws Exception {
        Member authMember = mock(Member.class);
        when(authMember.getId()).thenReturn(1L);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));


        Member member = Member.builder().studentId("123456789").nickname("testUser").build();
        MemberResponseDto memberResponseDto = MemberResponseDto.of(member);
        when(memberService.getMember(any(Member.class))).thenReturn(memberResponseDto);
        mockMvc.perform(get("/api/members").header("Auth",token).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("회원 가져오기 성공"))
                .andExpect(jsonPath("$.data.nickname").value(memberResponseDto.getNickname()))
                .andDo(print());
        verify(memberService).getMember(any(Member.class));
    }



    @Test
    @DisplayName("회원 닉네임/횃불이 사진 변경 테스트")
    void updateSuccessTest() throws Exception{
        Member authMember = mock(Member.class);
        when(authMember.getId()).thenReturn(1L);
        String token = "testToken";
        when(tokenProvider.resolveToken(any(HttpServletRequest.class))).thenReturn(token);
        when(tokenProvider.validateToken(token)).thenReturn(true);
        when(tokenProvider.getAuthentication(token))
                .thenReturn(new UsernamePasswordAuthenticationToken(authMember, "", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        MemberUpdateNicknameDto memberUpdateNicknameDto = MemberUpdateNicknameDto.builder().nickname("changedName").fireId(5L).build();
        Long memberId = 1L;
        when(memberService.updateMemberNicknameFireId(any(Long.class),any(MemberUpdateNicknameDto.class))).thenReturn(memberId);
        String body = objectMapper.writeValueAsString(memberUpdateNicknameDto);
        mockMvc.perform(put("/api/members").header("Auth","testToken").content(body).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("회원 닉네임/횃불이 이미지 변경 성공"))
                .andExpect(jsonPath("$.data").value(memberId))
                .andDo(print());

        verify(memberService).updateMemberNicknameFireId(any(Long.class),any(MemberUpdateNicknameDto.class));
    }






}
