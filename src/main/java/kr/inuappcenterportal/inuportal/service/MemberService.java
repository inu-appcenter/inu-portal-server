package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.MemberLoginDto;
import kr.inuappcenterportal.inuportal.dto.MemberSaveDto;
import kr.inuappcenterportal.inuportal.dto.MemberUpdateDto;
import kr.inuappcenterportal.inuportal.exception.ex.MyDuplicateException;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyNotFoundException;
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
            throw new MyDuplicateException(MyErrorCode.USER_DUPLICATE_EMAIL);
        }
        Member member= Member.builder().email(memberSaveDto.getEmail()).password(encodedPassword).roles(Collections.singletonList("ROLE_USER")).build();
        return memberRepository.save(member).getId();
    }

    @Transactional
    public Long changePassword(Long id, MemberUpdateDto memberUpdateDto){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        String encodedPassword = passwordEncoder.encode(memberUpdateDto.getPassword());
        member.update(encodedPassword);
        return member.getId();
    }

    @Transactional
    public void delete(Long id){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        memberRepository.delete(member);
    }

    @Transactional
    public String login(MemberLoginDto memberLoginDto){
        Member member = memberRepository.findByEmail(memberLoginDto.getEmail()).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));

        if(!passwordEncoder.matches(memberLoginDto.getPassword(),member.getPassword())){
            throw new NotFoundException("비밀번호가 틀립니다.");
        }
        return tokenProvider.createToken(String.valueOf(member.getId()),member.getRoles());
    }

    @Transactional(readOnly = true)
    public String getMember(Long id){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        return member.getEmail();
    }
}
