package kr.inuappcenterportal.inuportal.service;



import kr.inuappcenterportal.inuportal.domain.member.dto.LoginDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberUpdateNicknameDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private SchoolLoginRepository schoolLoginRepository;


    @Test
    @DisplayName("로그인 성공 테스트")
    public void loginSuccessTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        String studentId = "20241234";
        String password = "12345";
        Member member = createMember(studentId);
        LoginDto loginDto = LoginDto.builder().studentId(studentId).password(password).build();
        when(schoolLoginRepository.loginCheck(studentId,password)).thenReturn(true);
        when(memberRepository.existsByStudentId(studentId)).thenReturn(false);
        when(memberRepository.findByStudentId(studentId)).thenReturn(Optional.ofNullable(member));
        when(tokenProvider.createToken(eq("1"),eq(member.getRoles()), any(LocalDateTime.class))).thenReturn("testAccessToken");
        when(tokenProvider.createRefreshToken(eq("1"), any(LocalDateTime.class))).thenReturn("testRefreshToken");

        //when
        TokenDto tokenDto = memberService.schoolLogin(loginDto);

        //then

        assertAll(
                ()->assertEquals(tokenDto.getAccessToken(),"testAccessToken"),
                ()->assertEquals(tokenDto.getAccessToken(),"testAccessToken")
        );

        verify(schoolLoginRepository,times(1)).loginCheck(studentId,password);
        verify(memberRepository, times(1)).existsByStudentId(studentId);
        verify(memberRepository, times(1)).findByStudentId(studentId);
    }

    @Test
    @DisplayName("로그인 실패 테스트")
    public void loginFailTest(){
        //given
        String studentId = "20241234";
        String password = "12345";
        LoginDto loginDto = LoginDto.builder().studentId(studentId).password(password).build();
        when(schoolLoginRepository.loginCheck(studentId,password)).thenReturn(false);

        //when
        MyException exception = assertThrows(MyException.class,()->memberService.schoolLogin(loginDto));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"학번 또는 비밀번호가 틀립니다.");
        verify(schoolLoginRepository,times(1)).loginCheck(studentId,password);
    }


    @Test
    @DisplayName("신규 회원 생성 테스트")
    public void createMemberTest(){
        //given
        String schoolNumber= "201900000";

        //when
        memberService.createMember(schoolNumber);

        //then
        verify(memberRepository).save(any());
    }

    @Test
    @DisplayName("토큰 발급 테스트")
    public void createToken() throws NoSuchFieldException, IllegalAccessException {
        //given
        String studentId = "20241234";
        Member member = createMember(studentId);
        when(tokenProvider.createToken(eq("1"),eq(member.getRoles()), any(LocalDateTime.class))).thenReturn("testAccessToken");
        when(tokenProvider.createRefreshToken(eq("1"), any(LocalDateTime.class))).thenReturn("testRefreshToken");

        //when
        TokenDto tokenDto = memberService.login(member);

        //then
        assertAll(
                ()->assertEquals(tokenDto.getAccessToken(),"testAccessToken"),
                ()->assertEquals(tokenDto.getAccessToken(),"testAccessToken")
        );

        verify(tokenProvider, times(1)).createToken(eq("1"), eq(member.getRoles()), any(LocalDateTime.class));
        verify(tokenProvider, times(1)).createRefreshToken(eq("1"), any(LocalDateTime.class));
    }
    private Member createMember(String studentId) throws NoSuchFieldException, IllegalAccessException {
        Member member = Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build();
        ReflectionTestUtils.setField(member,"id",1L);
        return member;
    }

    @Test
    @DisplayName("토큰 재발급 성공 테스트")
    public void refreshTokenTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        String token = "testToken";
        when(tokenProvider.validateRefreshToken(token)).thenReturn(true);
        when(tokenProvider.getUsernameByRefresh(token)).thenReturn("1");
        String studentId = "20241234";
        Member member = createMember(studentId);
        when(tokenProvider.createToken(eq("1"),eq(member.getRoles()), any(LocalDateTime.class))).thenReturn("testAccessToken");
        when(tokenProvider.createRefreshToken(eq("1"), any(LocalDateTime.class))).thenReturn("testRefreshToken");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        //when
        TokenDto tokenDto = memberService.refreshToken(token);

        //then
        assertAll(
                ()->assertEquals(tokenDto.getAccessToken(),"testAccessToken"),
                ()->assertEquals(tokenDto.getAccessToken(),"testAccessToken")
        );

        verify(memberRepository, times(1)).findById(1L);
        verify(tokenProvider, times(1)).validateRefreshToken(token);
        verify(tokenProvider, times(1)).getUsernameByRefresh(token);
    }

    @Test
    @DisplayName("토큰 재발급 실패 테스트")
    public void refreshFailTest(){
        //given
        String token = "testToken";
        when(tokenProvider.validateRefreshToken(token)).thenReturn(false);

        //when
        MyException exception = assertThrows(MyException.class, ()->memberService.refreshToken(token));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"만료된 토큰입니다.");
    }

    @Test
    @DisplayName("회원 정보 수정 성공 테스트")
    public void updateMemberSuccessTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        long id = 1L;
        String nickname = "newNickname";
        Long fire = 3L;
        MemberUpdateNicknameDto dto = MemberUpdateNicknameDto.builder().nickname(nickname).fireId(fire).build();
        Member member = createMember("20241234");
        when(memberRepository.findById(id)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(nickname)).thenReturn(false);

        //when
        Long memberId = memberService.updateMemberNicknameFireId(id,dto);

        //then
        assertEquals(memberId,id);
        verify(memberRepository, times(1)).findById(id);
        verify(memberRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("회원 정보 수정 실패 테스트 - 중복 닉네임")
    public void updateMemberFailDuplicatedNicknameTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        long id = 1L;
        String nickname = "newNickname";
        Long fire = 3L;
        MemberUpdateNicknameDto dto = MemberUpdateNicknameDto.builder().nickname(nickname).fireId(fire).build();
        Member member = createMember("20241234");
        when(memberRepository.findById(id)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(nickname)).thenReturn(true);

        //when
        MyException exception = assertThrows(MyException.class, ()->memberService.updateMemberNicknameFireId(id,dto));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"동일한 닉네임이 존재합니다.");
        verify(memberRepository, times(1)).findById(id);
        verify(memberRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("회원 정보 수정 실패 테스트 - 공백 닉네임")
    public void updateMemberFailBlankNicknameTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        long id = 1L;
        String nickname = "      ";
        Long fire = 3L;
        MemberUpdateNicknameDto dto = MemberUpdateNicknameDto.builder().nickname(nickname).fireId(fire).build();
        Member member = createMember("20241234");
        when(memberRepository.findById(id)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname(nickname)).thenReturn(false);

        //when
        MyException exception = assertThrows(MyException.class, ()->memberService.updateMemberNicknameFireId(id,dto));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"닉네임이 빈칸 혹은 공백입니다.");
        verify(memberRepository, times(1)).findById(id);
        verify(memberRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("회원 정보 수정 실패 테스트 - 공백 요청")
    public void updateMemberFailNullTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        long id = 1L;
        MemberUpdateNicknameDto dto = MemberUpdateNicknameDto.builder().build();
        Member member = createMember("20241234");
        when(memberRepository.findById(id)).thenReturn(Optional.of(member));

        //when
        MyException exception = assertThrows(MyException.class, ()->memberService.updateMemberNicknameFireId(id,dto));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"닉네임, 횃불이 아이디 모두 공백입니다.");
        verify(memberRepository, times(1)).findById(id);
        verify(memberRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("회원 삭제 테스트")
    public void deleteMemberTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        Member member = createMember("20241234");
        //when
        memberService.delete(member);

        //then
        verify(memberRepository, times(1)).delete(member);
    }

    @Test
    @DisplayName("회원 정보 가져오기 테스트 - 일반회원")
    public void getMemberUserTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        Long fire = 3L;
        String studentId = "20231234";
        Member member = createMember(studentId);
        member.updateFire(fire);

        //when
        MemberResponseDto memberResponseDto = memberService.getMember(member);

        //given
        assertAll(
                ()->assertEquals(memberResponseDto.getFireId(),fire),
                ()->assertEquals(memberResponseDto.getId(),1L),
                ()->assertEquals(memberResponseDto.getNickname(),studentId),
                ()->assertEquals(memberResponseDto.getRole(),"user")
        );
    }

    @Test
    @DisplayName("회원 정보 가져오기 테스트 - 관리자")
    public void getMemberAdminTest() throws NoSuchFieldException, IllegalAccessException {
        //given
        Long fire = 3L;
        String studentId = "20231234";
        Member member = createMember(studentId);
        ReflectionTestUtils.setField(member,"roles",Collections.singletonList("ROLE_ADMIN"));
        member.updateFire(fire);

        //when
        MemberResponseDto memberResponseDto = memberService.getMember(member);

        //given
        assertAll(
                ()->assertEquals(memberResponseDto.getFireId(),fire),
                ()->assertEquals(memberResponseDto.getId(),1L),
                ()->assertEquals(memberResponseDto.getNickname(),studentId),
                ()->assertEquals(memberResponseDto.getRole(),"admin")
        );
    }




}
