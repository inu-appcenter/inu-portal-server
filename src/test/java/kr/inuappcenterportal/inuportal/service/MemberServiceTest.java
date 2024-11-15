package kr.inuappcenterportal.inuportal.service;


import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.oracleRepository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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

    private Member member;

    @BeforeEach
    void set(){
        member = Member.builder().nickname("팁쟁이").studentId("201900000").roles(Collections.singletonList("ROLE_USER")).build();
    }

    @Test
    @DisplayName("신규 회원 생성")
    public void createMember(){
        String schoolNumber= "201900000";
        memberService.createMember(schoolNumber);
        verify(memberRepository).save(any());
    }


}
