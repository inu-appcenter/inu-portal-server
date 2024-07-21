package kr.inuappcenterportal.inuportal.service;

import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.*;
import kr.inuappcenterportal.inuportal.exception.ex.*;
import kr.inuappcenterportal.inuportal.oracleRepository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final SchoolLoginRepository schoolLoginRepository;


    @Transactional
    public Long updateMemberNicknameFireId(Long id, MemberUpdateNicknameDto memberUpdateNicknameDto){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyException(MyErrorCode.USER_NOT_FOUND));
        if(memberUpdateNicknameDto.getNickname()!=null) {
            if (memberUpdateNicknameDto.getNickname().equals(member.getNickname())) {
                throw new MyException(MyErrorCode.SAME_NICKNAME_UPDATE);
            }
            if (memberRepository.existsByNickname(memberUpdateNicknameDto.getNickname())) {
                throw new MyException(MyErrorCode.USER_DUPLICATE_NICKNAME);
            }
            if(memberUpdateNicknameDto.getNickname().trim().isEmpty()){
                throw new MyException(MyErrorCode.NOT_BLANK_NICKNAME);
            }
            if(memberUpdateNicknameDto.getFireId()!=null){
                member.updateNicknameAndFire(memberUpdateNicknameDto.getNickname(),memberUpdateNicknameDto.getFireId());
            }
            else{
                member.updateNickName(memberUpdateNicknameDto.getNickname());
            }
        }else if(memberUpdateNicknameDto.getFireId()!=null){
            member.updateFire(memberUpdateNicknameDto.getFireId());
        }
        else{
            throw new MyException(MyErrorCode.EMPTY_REQUEST);
        }
        return member.getId();
    }

    @Transactional
    public void delete(Member member){
        memberRepository.delete(member);
    }

    @Transactional
    public TokenDto login(Member member){
        LocalDateTime localDateTime = LocalDateTime.now();
        long tokenValidMillisecond = 1000L * 60 * 60 * 2 ;//2시간
        long refreshValidMillisecond = 1000L * 60 *60 *24;//24시간
        String accessToken = tokenProvider.createToken(member.getId().toString(),member.getRoles(),localDateTime);
        String refreshToken = tokenProvider.createRefreshToken(member.getId().toString(),localDateTime);
        return TokenDto.of(accessToken,refreshToken,localDateTime.plus(Duration.ofMillis(tokenValidMillisecond)).toString(),localDateTime.plus(Duration.ofMillis(refreshValidMillisecond)).toString());
    }

    @Transactional(readOnly = true)
    public TokenDto refreshToken(String token){
        Long id = Long.valueOf(tokenProvider.getUsernameByRefresh(token));
        if(!tokenProvider.validateRefreshToken(token)){
            throw new MyException(MyErrorCode.EXPIRED_TOKEN);
        }
        Member member = memberRepository.findById(id).orElseThrow(()->new MyException(MyErrorCode.USER_NOT_FOUND));
        LocalDateTime localDateTime = LocalDateTime.now();
        long tokenValidMillisecond = 1000L * 60 * 60 * 2 ;//2시간
        long refreshValidMillisecond = 1000L * 60 *60 *24;//24시간
        String accessToken = tokenProvider.createToken(member.getId().toString(),member.getRoles(),localDateTime);
        String refreshToken = tokenProvider.createRefreshToken(member.getId().toString(),localDateTime);
        return TokenDto.of(accessToken,refreshToken,localDateTime.plus(Duration.ofMillis(tokenValidMillisecond)).toString(),localDateTime.plus(Duration.ofMillis(refreshValidMillisecond)).toString());
    }


    @Transactional(readOnly = true)
    public MemberResponseDto getMember(Member member){
        return MemberResponseDto.of(member);
    }

    @Transactional(readOnly = true)
    public List<MemberResponseDto> getAllMember(){
        return memberRepository.findAll().stream().map(MemberResponseDto::of).collect(Collectors.toList());
    }

    @Transactional
    public TokenDto schoolLogin(LoginDto loginDto){
        //서버 관련 문제로 일시적 관리자 로그인 추가
        if(memberRepository.existsByStudentId(loginDto.getStudentId()) && memberRepository.findByStudentId(loginDto.getStudentId()).get().getRoles().get(0).equals("ROLE_ADMIN")){
            Member member = memberRepository.findByStudentId(loginDto.getStudentId()).get();
            return login(member);
        }
        else {
            if (!schoolLoginRepository.loginCheck(loginDto.getStudentId(), loginDto.getPassword())) {
                throw new MyException(MyErrorCode.STUDENT_LOGIN_ERROR);
            }
            if (!memberRepository.existsByStudentId(loginDto.getStudentId())) {
                createMember(loginDto.getStudentId());
            }
            Member member = memberRepository.findByStudentId(loginDto.getStudentId()).orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
            return login(member);
        }
    }
    @Transactional
    public void createMember(String studentId){
        Member member = Member.builder().studentId(studentId).nickname(studentId).roles(Collections.singletonList("ROLE_USER")).build();
        memberRepository.save(member);
    }

}
