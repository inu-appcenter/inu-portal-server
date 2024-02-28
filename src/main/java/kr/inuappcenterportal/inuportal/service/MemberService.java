package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.*;
import kr.inuappcenterportal.inuportal.exception.ex.*;
import kr.inuappcenterportal.inuportal.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final MailService mailService;
    private final RedisService redisService;

    @Transactional
    public Long join(MemberSaveDto memberSaveDto){
        if(!checkSchoolEmail(memberSaveDto.getEmail())){
            throw new MyNotPermittedException(MyErrorCode.ONLY_SCHOOL_EMAIL);
        }
        String encodedPassword = passwordEncoder.encode(memberSaveDto.getPassword());
        if(memberRepository.existsByEmail(memberSaveDto.getEmail())){
            throw new MyDuplicateException(MyErrorCode.USER_DUPLICATE_EMAIL);
        }
        if(memberRepository.existsByNickname(memberSaveDto.getNickname())){
            throw new MyDuplicateException(MyErrorCode.USER_DUPLICATE_NICKNAME);
        }
        Member member= Member.builder().email(memberSaveDto.getEmail()).nickname(memberSaveDto.getNickname()).password(encodedPassword).roles(Collections.singletonList("ROLE_USER")).build();
        return memberRepository.save(member).getId();
    }

    @Transactional
    public Long updateMemberPassword(Long id, MemberUpdatePasswordDto memberUpdatePasswordDto){
       Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
       if(!passwordEncoder.matches(memberUpdatePasswordDto.getPassword(),member.getPassword())){
           throw new MyUnauthorizedException(MyErrorCode.PASSWORD_NOT_MATCHED);
       }
       String encodedPassword = passwordEncoder.encode(memberUpdatePasswordDto.getNewPassword());
       member.updatePassword(encodedPassword);
        return member.getId();
    }

    @Transactional
    public Long updateMemberNickname(Long id, MemberUpdateNicknameDto memberUpdateNicknameDto){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        if(memberUpdateNicknameDto.getNickname().equals(member.getNickname())){
            throw new MyDuplicateException(MyErrorCode.SAME_NICKNAME_UPDATE);
        }
        if(memberRepository.existsByNickname(memberUpdateNicknameDto.getNickname())){
            throw new MyDuplicateException(MyErrorCode.USER_DUPLICATE_NICKNAME);
        }
        member.updateNickname(memberUpdateNicknameDto.getNickname());
        return member.getId();
    }

    @Transactional
    public void delete(Long id){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        memberRepository.delete(member);
    }

    @Transactional
    public String login(MemberLoginDto memberLoginDto){
        Member member = memberRepository.findByEmail(memberLoginDto.getEmail()).orElseThrow(()->new MyUnauthorizedException(MyErrorCode.ID_NOT_FOUND));

        if(!passwordEncoder.matches(memberLoginDto.getPassword(),member.getPassword())){
            throw new MyUnauthorizedException(MyErrorCode.PASSWORD_NOT_MATCHED);
        }
        return tokenProvider.createToken(String.valueOf(member.getId()),member.getRoles());
    }

    @Transactional(readOnly = true)
    public MemberResponseDto getMember(Long id){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyNotFoundException(MyErrorCode.USER_NOT_FOUND));
        return new MemberResponseDto(member);
    }

    @Transactional(readOnly = true)
    public List<MemberResponseDto> getAllMember(){
        return memberRepository.findAll().stream().map(MemberResponseDto::new).collect(Collectors.toList());
    }

    public String sendMail(EmailDto emailDto){
        if(!checkSchoolEmail(emailDto.getEmail())){
            throw new MyNotPermittedException(MyErrorCode.ONLY_SCHOOL_EMAIL);
        }
        if(memberRepository.existsByEmail(emailDto.getEmail())){
            throw new MyDuplicateException(MyErrorCode.USER_DUPLICATE_EMAIL);
        }
        String numbers = createNumber();
        mailService.sendMail(emailDto.getEmail(),numbers);
        redisService.storeMail(emailDto.getEmail(),numbers);
        return emailDto.getEmail();
    }
    public String createNumber(){
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i = 0 ; i < 6 ; i++){
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    public Boolean checkNumbers(EmailCheckDto emailCheckDto){
        return redisService.getNumbers(emailCheckDto.getEmail()).equals(emailCheckDto.getNumbers());
    }

    public Boolean checkSchoolEmail(String email){
        int atIndex = email.indexOf("@");
        if (atIndex == -1) {
            return false;
        }
        String domain = email.substring(atIndex + 1);
        return domain.equals("inu.ac.kr");
    }
}
