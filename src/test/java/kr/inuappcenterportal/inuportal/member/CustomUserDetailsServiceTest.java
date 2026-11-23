package kr.inuappcenterportal.inuportal.member;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.CustomUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("일반 회원은 토큰의 회원 ID로 인증 정보를 불러온다")
    void loadActiveMember() {
        Member member = createMember();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("1");

        assertThat(userDetails).isSameAs(member);
    }

    @Test
    @DisplayName("탈퇴한 회원은 남아 있는 토큰으로 인증할 수 없다")
    void rejectWithdrawnMember() {
        Member member = createMember();
        member.withdraw(LocalDateTime.now());
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("1"))
                .isInstanceOf(MyException.class)
                .extracting(e -> ((MyException) e).getErrorCode())
                .isEqualTo(MyErrorCode.WITHDRAWN_MEMBER);
    }

    private Member createMember() {
        Member member = Member.builder().studentId("20241234").roles(List.of("ROLE_USER")).build();
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }
}
