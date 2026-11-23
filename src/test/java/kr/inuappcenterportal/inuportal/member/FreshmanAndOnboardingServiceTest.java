package kr.inuappcenterportal.inuportal.member;

import kr.inuappcenterportal.inuportal.domain.dailyBrief.repository.DailyBriefSettingRepository;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.service.DailyBriefService;
import kr.inuappcenterportal.inuportal.domain.department.model.SchoolDepartment;
import kr.inuappcenterportal.inuportal.domain.department.repository.SchoolDepartmentRepository;
import kr.inuappcenterportal.inuportal.domain.member.dto.*;
import kr.inuappcenterportal.inuportal.domain.member.enums.MemberType;
import kr.inuappcenterportal.inuportal.domain.member.enums.RealNameSource;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.FreshmanRateLimiter;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.reply.repository.ReplyRepository;
import kr.inuappcenterportal.inuportal.domain.scrap.repository.ScrapRepository;
import kr.inuappcenterportal.inuportal.global.config.FreshmanSecurityProperties;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.security.FirebaseAppCheckVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FreshmanAndOnboardingServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SchoolLoginRepository schoolLoginRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Mock
    private SchoolDepartmentRepository schoolDepartmentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private ReplyRepository replyRepository;

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private DailyBriefSettingRepository dailyBriefSettingRepository;

    @Mock
    private DailyBriefService dailyBriefService;

    @Mock
    private FirebaseAppCheckVerifier firebaseAppCheckVerifier;

    @Mock
    private FreshmanRateLimiter freshmanRateLimiter;

    @Spy
    private FreshmanSecurityProperties freshmanSecurityProperties = new FreshmanSecurityProperties(
            true,
            "1234567890",
            "test-hmac-secret-key-at-least-32-bytes-long",
            null
    );

    @BeforeEach
    void stubTokenExpiryDefaults() {
        lenient().when(tokenProvider.createToken(any(), any(), any())).thenReturn("access_token");
        lenient().when(tokenProvider.createRefreshToken(any(), any())).thenReturn("refresh_token");
        lenient().when(tokenProvider.getAccessTokenExpiry(any())).thenReturn(LocalDateTime.now().plusHours(2));
        lenient().when(tokenProvider.getRefreshTokenExpiry(any())).thenReturn(LocalDateTime.now().plusDays(1));
    }

    @Test
    @DisplayName("학번 9자리 숫자는 STUDENT로 분류되고, 그 외는 FACULTY로 분류된다")
    void resolveMemberTypeTest() {
        assertEquals(MemberType.STUDENT, memberService.resolveMemberType("202612345"));
        assertEquals(MemberType.STUDENT, memberService.resolveMemberType("202001001"));
        assertEquals(MemberType.FACULTY, memberService.resolveMemberType("prof_lee"));
        assertEquals(MemberType.FACULTY, memberService.resolveMemberType("staff12"));
        assertEquals(MemberType.FACULTY, memberService.resolveMemberType("12345678"));
    }

    @Test
    @DisplayName("신입생 신규 가입 시 App Check 및 Rate Limit 검증 후 학과와 해시·만료일을 저장하고 토큰을 발급한다")
    void freshmanLoginSignupTest() {
        FreshmanLoginDto dto = new FreshmanLoginDto("202612345", "김신입", "20070101", "컴퓨터공학부");
        when(memberRepository.findByExamNo("202612345")).thenReturn(Optional.empty());
        SchoolDepartment cse = SchoolDepartment.create("CSE", "컴퓨터공학부", 2026, "10");
        when(schoolDepartmentRepository.findByCodeAndActiveTrue("컴퓨터공학부")).thenReturn(Optional.empty());
        when(schoolDepartmentRepository.findFirstByNameAndActiveTrue("컴퓨터공학부")).thenReturn(Optional.of(cse));

        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            ReflectionTestUtils.setField(m, "id", 100L);
            return m;
        });

        TokenDto result = memberService.freshmanLogin(dto, "valid-appcheck-token", "1.2.3.4");

        assertNotNull(result);
        assertEquals("access_token", result.getAccessToken());
        verify(freshmanRateLimiter).checkAttempt("1.2.3.4", "202612345");
        verify(firebaseAppCheckVerifier).verify("valid-appcheck-token");
        verify(freshmanRateLimiter).checkSignupLimit("1.2.3.4");
        verify(dailyBriefService).createDefaultSetting(any(Member.class));
        verify(memberRepository).save(argThat(m ->
                m.getMemberType() == MemberType.FRESHMAN
                        && m.getRoles().contains("ROLE_USER")
                        && m.getFreshmanVerifyHash() != null
                        && m.getFreshmanExpiresAt() != null
                        && cse.equals(m.getSchoolDepartment())
        ));
    }

    @Test
    @DisplayName("신입생 재로그인 시 수험번호만 같고 이름·생년월일 해시가 다르면 401 예외가 발생한다")
    void freshmanLoginHashMismatchBlockedTest() {
        FreshmanLoginDto dto = new FreshmanLoginDto("202612345", "사칭범", "20070102", "컴퓨터공학부");
        Member existing = Member.builder().studentId("F_202612345").roles(Collections.singletonList("ROLE_USER")).build();
        existing.setMemberType(MemberType.FRESHMAN);
        existing.setExamNo("202612345");
        existing.setFreshmanVerifyHash(memberService.computeFreshmanVerifyHash("김신입", "20070101"));
        existing.setFreshmanExpiresAt(LocalDateTime.now().plusMonths(2));
        ReflectionTestUtils.setField(existing, "id", 100L);

        when(memberRepository.findByExamNo("202612345")).thenReturn(Optional.of(existing));

        MyException ex = assertThrows(MyException.class,
                () -> memberService.freshmanLogin(dto, "valid-appcheck-token", "1.2.3.4"));
        assertEquals(MyErrorCode.FRESHMAN_AUTH_MISMATCH, ex.getErrorCode());
        verify(freshmanRateLimiter).recordExamFailure("202612345");
        verify(tokenProvider, never()).createToken(any(), any(), any());
    }

    @Test
    @DisplayName("유효기간이 만료된 신입생 계정은 재로그인 시 403 FRESHMAN_EXPIRED 예외가 발생한다")
    void freshmanLoginExpiredBlockedTest() {
        FreshmanLoginDto dto = new FreshmanLoginDto("202612345", "김신입", "20070101", "컴퓨터공학부");
        Member existing = Member.builder().studentId("F_202612345").roles(Collections.singletonList("ROLE_USER")).build();
        existing.setMemberType(MemberType.FRESHMAN);
        existing.setExamNo("202612345");
        existing.setFreshmanVerifyHash(memberService.computeFreshmanVerifyHash("김신입", "20070101"));
        existing.setFreshmanExpiresAt(LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(existing, "id", 100L);

        when(memberRepository.findByExamNo("202612345")).thenReturn(Optional.of(existing));

        MyException ex = assertThrows(MyException.class,
                () -> memberService.freshmanLogin(dto, "valid-appcheck-token", "1.2.3.4"));
        assertEquals(MyErrorCode.FRESHMAN_EXPIRED, ex.getErrorCode());
    }

    @Test
    @DisplayName("App Check 검증 실패 시 로그인·계정 생성이 차단된다")
    void freshmanLoginAppCheckFailedTest() {
        FreshmanLoginDto dto = new FreshmanLoginDto("202612345", "김신입", "20070101", "컴퓨터공학부");
        doThrow(new MyException(MyErrorCode.INVALID_APP_CHECK_TOKEN))
                .when(firebaseAppCheckVerifier).verify(any());

        MyException ex = assertThrows(MyException.class,
                () -> memberService.freshmanLogin(dto, "bad-token", "1.2.3.4"));
        assertEquals(MyErrorCode.INVALID_APP_CHECK_TOKEN, ex.getErrorCode());
        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("Rate Limit 초과 시 로그인 시도가 429로 차단된다")
    void freshmanLoginRateLimitedTest() {
        FreshmanLoginDto dto = new FreshmanLoginDto("202612345", "김신입", "20070101", "컴퓨터공학부");
        doThrow(new MyException(MyErrorCode.FRESHMAN_RATE_LIMITED))
                .when(freshmanRateLimiter).checkAttempt("1.2.3.4", "202612345");

        MyException ex = assertThrows(MyException.class,
                () -> memberService.freshmanLogin(dto, "token", "1.2.3.4"));
        assertEquals(MyErrorCode.FRESHMAN_RATE_LIMITED, ex.getErrorCode());
        verify(firebaseAppCheckVerifier, never()).verify(any());
    }

    @Test
    @DisplayName("온보딩 완료 시 닉네임, 실명, 학과가 정상 저장되고 onboardingCompleted가 true가 된다")
    void completeOnboardingTest() {
        Long memberId = 1L;
        Member member = Member.builder().studentId("202612345").roles(List.of("ROLE_USER")).build();
        ReflectionTestUtils.setField(member, "id", memberId);

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.existsByNickname("횃불이")).thenReturn(false);

        SchoolDepartment schoolDept = SchoolDepartment.create("CSE", "컴퓨터공학부", 2026, "10");
        when(schoolDepartmentRepository.findByCodeAndActiveTrue("CSE")).thenReturn(Optional.of(schoolDept));

        OnboardingRequestDto dto = new OnboardingRequestDto(
                "횃불이",
                1L,
                "홍길동",
                RealNameSource.PORTAL_SCRAPE,
                "CSE",
                true
        );

        MemberResponseDto response = memberService.completeOnboarding(memberId, dto);

        assertNotNull(response);
        assertEquals("횃불이", response.getNickname());
        assertEquals("홍길동", response.getRealName());
        assertEquals("컴퓨터공학부", response.getDepartment());
        assertTrue(response.getOnboardingCompleted());
        assertFalse(response.isNeedsOnboarding());
    }

    @Test
    @DisplayName("3월 학번 첫 로그인 시 빈 STUDENT 레코드를 제거하고 기존 FRESHMAN 레코드를 정규 학번으로 승격한 뒤 새 JWT를 반환한다")
    void checkAndMergeFreshmanInPlaceUpgradeTest() {
        Long currentMemberId = 200L;
        Member currentMember = Member.builder().studentId("202701234").roles(List.of("ROLE_USER")).build();
        currentMember.setMemberType(MemberType.STUDENT);
        ReflectionTestUtils.setField(currentMember, "id", currentMemberId);

        Long freshmanId = 100L;
        Member freshman = Member.builder().studentId("F_202799999").roles(List.of("ROLE_USER")).build();
        freshman.setMemberType(MemberType.FRESHMAN);
        freshman.setExamNo("202799999");
        freshman.setFreshmanVerifyHash(memberService.computeFreshmanVerifyHash("홍길동", "20080101"));
        freshman.setFreshmanExpiresAt(LocalDateTime.now().plusDays(15));
        freshman.updateNicknameAndFire("신입이", 3L);
        ReflectionTestUtils.setField(freshman, "id", freshmanId);

        SchoolDepartment cse = SchoolDepartment.create("CSE", "컴퓨터공학부", 2027, "10");
        when(schoolDepartmentRepository.findByCodeAndActiveTrue("컴퓨터공학부")).thenReturn(Optional.empty());
        when(schoolDepartmentRepository.findFirstByNameAndActiveTrue("컴퓨터공학부")).thenReturn(Optional.of(cse));

        when(memberRepository.findById(currentMemberId)).thenReturn(Optional.of(currentMember));
        when(memberRepository.findFreshmanByVerifyHash(freshman.getFreshmanVerifyHash()))
                .thenReturn(Optional.of(freshman));
        when(postRepository.existsByMember(currentMember)).thenReturn(false);
        when(replyRepository.existsByMember(currentMember)).thenReturn(false);
        when(scrapRepository.existsByMember(currentMember)).thenReturn(false);

        when(tokenProvider.createToken(eq("100"), any(), any())).thenReturn("upgraded_access_token");

        MergeFreshmanRequestDto dto = new MergeFreshmanRequestDto("홍길동", "20080101", "컴퓨터공학부");

        MergeFreshmanResponseDto response = memberService.checkAndMergeFreshman(currentMemberId, dto);

        assertNotNull(response);
        assertTrue(response.getMerged());
        assertEquals("202799999", response.getExamNo());
        assertNotNull(response.getToken());
        assertEquals("upgraded_access_token", response.getToken().getAccessToken());

        // 빈 신규 STUDENT 레코드와 기본 설정만 삭제되고, FRESHMAN 레코드는 보존·승격된다
        verify(dailyBriefSettingRepository).deleteByMember(currentMember);
        verify(dailyBriefSettingRepository).flush();
        verify(memberRepository).delete(currentMember);
        verify(memberRepository).flush();
        verify(memberRepository, never()).delete(freshman);

        assertEquals("202701234", freshman.getStudentId());
        assertEquals(MemberType.STUDENT, freshman.getMemberType());
        assertEquals("신입이", freshman.getNickname());
        assertEquals(3L, freshman.getFireId());
        assertEquals(cse, freshman.getSchoolDepartment());
        assertTrue(freshman.getOnboardingCompleted());
        assertNull(freshman.getFreshmanExpiresAt());
        assertNull(freshman.getFreshmanVerifyHash());
    }

    @Test
    @DisplayName("신입생 인앱 전환 성공 시 현재 FRESHMAN 레코드가 정식 학번 STUDENT 로 승격되고 새 JWT 를 반환한다")
    void convertFreshmanToStudentSuccessTest() {
        Long freshmanId = 100L;
        Member freshman = Member.builder().studentId("F_202799999").roles(List.of("ROLE_USER")).build();
        freshman.setMemberType(MemberType.FRESHMAN);
        freshman.setExamNo("202799999");
        freshman.updateNicknameAndFire("신입이", 4L);
        ReflectionTestUtils.setField(freshman, "id", freshmanId);

        when(memberRepository.findById(freshmanId)).thenReturn(Optional.of(freshman));
        when(schoolLoginRepository.loginCheck("202701234", "pw1234")).thenReturn(true);
        when(schoolLoginRepository.resolveRoles("202701234")).thenReturn(List.of("ROLE_USER"));
        when(memberRepository.findByStudentId("202701234")).thenReturn(Optional.empty());
        when(tokenProvider.createToken(eq("100"), any(), any())).thenReturn("converted_access_token");

        TokenDto token = memberService.convertFreshmanToStudent(freshmanId, new LoginDto("202701234", "pw1234"));

        assertNotNull(token);
        assertEquals("converted_access_token", token.getAccessToken());
        assertEquals("202701234", freshman.getStudentId());
        assertEquals(MemberType.STUDENT, freshman.getMemberType());
        assertTrue(freshman.getIsAcademicVerified());
        assertNull(freshman.getFreshmanExpiresAt());
        assertNull(freshman.getFreshmanVerifyHash());
    }

    @Test
    @DisplayName("신입생 인앱 전환 시 포털 비밀번호가 틀리면 STUDENT_LOGIN_ERROR 예외가 발생한다")
    void convertFreshmanToStudentWrongPasswordTest() {
        Long freshmanId = 100L;
        Member freshman = Member.builder().studentId("F_202799999").roles(List.of("ROLE_USER")).build();
        freshman.setMemberType(MemberType.FRESHMAN);
        ReflectionTestUtils.setField(freshman, "id", freshmanId);

        when(memberRepository.findById(freshmanId)).thenReturn(Optional.of(freshman));
        when(schoolLoginRepository.loginCheck("202701234", "wrong")).thenReturn(false);

        MyException ex = assertThrows(MyException.class,
                () -> memberService.convertFreshmanToStudent(freshmanId, new LoginDto("202701234", "wrong")));
        assertEquals(MyErrorCode.STUDENT_LOGIN_ERROR, ex.getErrorCode());
    }

    @Test
    @DisplayName("신입생이 아닌 계정이 인앱 전환을 호출하면 FRESHMAN_CONVERT_NOT_ALLOWED 예외가 발생한다")
    void convertFreshmanToStudentNotFreshmanTest() {
        Long studentId = 200L;
        Member student = Member.builder().studentId("202701234").roles(List.of("ROLE_USER")).build();
        student.setMemberType(MemberType.STUDENT);
        ReflectionTestUtils.setField(student, "id", studentId);

        when(memberRepository.findById(studentId)).thenReturn(Optional.of(student));

        MyException ex = assertThrows(MyException.class,
                () -> memberService.convertFreshmanToStudent(studentId, new LoginDto("202701234", "pw")));
        assertEquals(MyErrorCode.FRESHMAN_CONVERT_NOT_ALLOWED, ex.getErrorCode());
    }

    @Test
    @DisplayName("신입생 인앱 전환 시 동일 학번의 빈 STUDENT 레코드가 이미 있으면 삭제 후 FRESHMAN 을 승격한다")
    void convertFreshmanToStudentDeletesEmptyExistingStudentTest() {
        Long freshmanId = 100L;
        Member freshman = Member.builder().studentId("F_202799999").roles(List.of("ROLE_USER")).build();
        freshman.setMemberType(MemberType.FRESHMAN);
        ReflectionTestUtils.setField(freshman, "id", freshmanId);

        Long existingId = 200L;
        Member existingStudent = Member.builder().studentId("202701234").roles(List.of("ROLE_USER")).build();
        existingStudent.setMemberType(MemberType.STUDENT);
        ReflectionTestUtils.setField(existingStudent, "id", existingId);

        when(memberRepository.findById(freshmanId)).thenReturn(Optional.of(freshman));
        when(schoolLoginRepository.loginCheck("202701234", "pw1234")).thenReturn(true);
        when(schoolLoginRepository.resolveRoles("202701234")).thenReturn(List.of("ROLE_USER"));
        when(memberRepository.findByStudentId("202701234")).thenReturn(Optional.of(existingStudent));
        when(postRepository.existsByMember(existingStudent)).thenReturn(false);
        when(replyRepository.existsByMember(existingStudent)).thenReturn(false);
        when(scrapRepository.existsByMember(existingStudent)).thenReturn(false);
        when(tokenProvider.createToken(eq("100"), any(), any())).thenReturn("converted_access_token");

        TokenDto token = memberService.convertFreshmanToStudent(freshmanId, new LoginDto("202701234", "pw1234"));

        assertEquals("converted_access_token", token.getAccessToken());
        verify(dailyBriefSettingRepository).deleteByMember(existingStudent);
        verify(memberRepository).delete(existingStudent);
        verify(memberRepository, never()).delete(freshman);
        assertEquals("202701234", freshman.getStudentId());
        assertEquals(MemberType.STUDENT, freshman.getMemberType());
    }
}
