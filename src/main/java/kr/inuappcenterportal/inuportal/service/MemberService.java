package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.MemberLoginDto;
import kr.inuappcenterportal.inuportal.dto.MemberSaveDto;
import kr.inuappcenterportal.inuportal.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webjars.NotFoundException;

import java.util.Collections;


@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional
    public Long join(MemberSaveDto memberSaveDto){
        String encodedPassword = passwordEncoder.encode(memberSaveDto.getPassword());

        if(memberRepository.existsByEmail(memberSaveDto.getEmail())){
            //throw new MyDuplicateException(MyErrorCode.USER_DUPLICATE_EMAIL);
            throw new DataIntegrityViolationException("이미 존재하는 이메일입니다.");
        }
        Member member= Member.builder().email(memberSaveDto.getEmail()).password(encodedPassword).roles(Collections.singletonList("ROLE_USER")).build();
        return memberRepository.save(member).getId();
    }

    @Transactional
    public Long changePassword(Long id, String password){
        Member member = memberRepository.findById(id).orElseThrow(()->new NotFoundException("존재하지 않는 회원입니다."));
        String encodedPassword = passwordEncoder.encode(password);
        member.update(encodedPassword);
        return member.getId();
    }

    @Transactional
    public void delete(Long id){
        Member member = memberRepository.findById(id).orElseThrow(()->new NotFoundException("존재하지 않는 회원입니다."));
        memberRepository.delete(member);
    }

    @Transactional
    public String login(MemberLoginDto memberLoginDto){
        Member member = memberRepository.findByEmail(memberLoginDto.getEmail()).orElseThrow(()->new NotFoundException("존재하지 않는 회원입니다."));

        if(!passwordEncoder.matches(memberLoginDto.getPassword(),member.getPassword())){
            throw new NotFoundException("비밀번호가 틀립니다.");
        }
        return tokenProvider.createToken(String.valueOf(member.getId()),member.getRoles());
    }

    @Transactional(readOnly = true)
    public String getMember(Long id){
        Member member = memberRepository.findById(id).orElseThrow(()->new NotFoundException("존재하지 않는 회원입니다."));
        return member.getEmail();
    }
}
