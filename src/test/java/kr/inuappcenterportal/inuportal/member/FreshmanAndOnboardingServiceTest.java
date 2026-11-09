package kr.inuappcenterportal.inuportal.member;

import kr.inuappcenterportal.inuportal.domain.department.model.SchoolDepartment;
import kr.inuappcenterportal.inuportal.domain.department.repository.SchoolDepartmentRepository;
import kr.inuappcenterportal.inuportal.domain.member.dto.*;
import kr.inuappcenterportal.inuportal.domain.member.enums.MemberType;
import kr.inuappcenterportal.inuportal.domain.member.enums.RealNameSource;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.reply.repository.ReplyRepository;
import kr.inuappcenterportal.inuportal.domain.scrap.repository.ScrapRepository;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FreshmanAndOnboardingServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

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

    @Test
    @DisplayName("학번 9자리 숫자는 STUDENT로 분류되고, 그 외는 FACULTY로 분류된다")
    void resolveMemberTypeTest() {
        assertEquals(MemberType.STUDENT, memberService.resolveMemberType("202612345"));
        assertEquals(MemberType.STUDENT, memberService.resolveMemberType("202001001"));
        assertEquals(MemberType.FACULTY, memberService.resolveMemberType("prof_lee"));
        assertEquals(MemberType.FACULTY, memberService.resolveMemberType("staff12"));
        assertEquals(MemberType.FACULTY, memberService.resolveMemberType("12345678")); // 8자리
    }

    @Test
    @DisplayName("신입생 로그인 시 수험번호로 신입생 계정을 생성하고 토큰을 발급한다")
    void freshmanLoginTest() {
        // given
        FreshmanLoginDto dto = new FreshmanLoginDto("202612345", "김신입", "20070101", "컴퓨터공학부");
        when(memberRepository.findByExamNo(dto.getExamNo())).thenReturn(Optional.empty());

        Member savedFreshman = Member.builder()
                .studentId("F_202612345")
                .roles(Collections.singletonList("ROLE_USER"))
                .build();
        savedFreshman.setMemberType(MemberType.FRESHMAN);
        savedFreshman.setExamNo(dto.getExamNo());
        savedFreshman.setBirthDate(dto.getBirthDate());
        ReflectionTestUtils.setField(savedFreshman, "id", 100L);

        when(memberRepository.save(any(Member.class))).thenReturn(savedFreshman);
        when(tokenProvider.createToken(any(), any(), any())).thenReturn("access_token");
        when(tokenProvider.createRefreshToken(any(), any())).thenReturn("refresh_token");
        when(tokenProvider.getAccessTokenExpiry(any())).thenReturn(LocalDateTime.now().plusHours(2));
        when(tokenProvider.getRefreshTokenExpiry(any())).thenReturn(LocalDateTime.now().plusDays(1));

        // when
        TokenDto result = memberService.freshmanLogin(dto);

        // then
        assertNotNull(result);
        assertEquals("access_token", result.getAccessToken());
        verify(memberRepository, times(1)).save(any(Member.class));
    }

    @Test
    @DisplayName("온보딩 완료 시 닉네임, 실명, 학과가 정상 저장되고 onboardingCompleted가 true가 된다")
    void completeOnboardingTest() {
        // given
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

        // when
        MemberResponseDto response = memberService.completeOnboarding(memberId, dto);

        // then
        assertNotNull(response);
        assertEquals("횃불이", response.getNickname());
        assertEquals("홍길동", response.getRealName());
        assertEquals("컴퓨터공학부", response.getDepartment());
        assertTrue(response.getOnboardingCompleted());
        assertFalse(response.isNeedsOnboarding());
    }

    @Test
    @DisplayName("3월 학번 첫 로그인 시 공식 실명과 생년월일이 일치하는 신입생 계정이 100% 무조건 통합된다")
    void checkAndMergeFreshmanTest() {
        // given
        Long currentMemberId = 2L;
        Member currentMember = Member.builder().studentId("202601234").roles(List.of("ROLE_USER")).build();
        currentMember.setMemberType(MemberType.STUDENT);
        ReflectionTestUtils.setField(currentMember, "id", currentMemberId);

        Long freshmanId = 1L;
        Member freshman = Member.builder().studentId("F_202699999").roles(List.of("ROLE_USER")).build();
        freshman.setMemberType(MemberType.FRESHMAN);
        freshman.setExamNo("202699999");
        freshman.updateNickName("신입이");
        freshman.updateFire(3L);
        ReflectionTestUtils.setField(freshman, "id", freshmanId);

        when(memberRepository.findById(currentMemberId)).thenReturn(Optional.of(currentMember));
        when(memberRepository.findFreshmanByRealNameAndBirthDate("홍길동", "20070101"))
                .thenReturn(Optional.of(freshman));

        MergeFreshmanRequestDto dto = new MergeFreshmanRequestDto("홍길동", "20070101", "컴퓨터공학부");

        // when
        MergeFreshmanResponseDto response = memberService.checkAndMergeFreshman(currentMemberId, dto);

        // then
        assertNotNull(response);
        assertTrue(response.getMerged());
        assertEquals("202699999", response.getExamNo());

        // 소유권 이전 검증
        verify(postRepository, times(1)).transferMember(freshman, currentMember);
        verify(replyRepository, times(1)).transferMember(freshman, currentMember);
        verify(scrapRepository, times(1)).transferMember(freshman, currentMember);

        // 신입생 임시 계정 삭제 검증
        verify(memberRepository, times(1)).delete(freshman);

        // 닉네임과 캐릭터 계승 검증
        assertEquals("신입이", currentMember.getNickname());
        assertEquals(3L, currentMember.getFireId());
        assertTrue(currentMember.getOnboardingCompleted());
    }
}
