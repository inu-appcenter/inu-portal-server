package kr.inuappcenterportal.inuportal.global.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findById(Long.valueOf(username)).orElseThrow(()-> new MyException(MyErrorCode.USER_NOT_FOUND));
        // 탈퇴한 회원은 남아 있는 토큰으로도 접근할 수 없다. HTTP 요청과 웹소켓(StompHandler) 인증이 모두 여기를 거친다.
        if (member.isWithdrawn()) {
            throw new MyException(MyErrorCode.WITHDRAWN_MEMBER);
        }
        return member;
    }
}
