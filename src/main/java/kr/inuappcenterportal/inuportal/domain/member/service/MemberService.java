package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.dto.LoginDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberUpdateNicknameDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final SchoolLoginRepository schoolLoginRepository;


    @Transactional
    public Long updateMemberNicknameFireId(Long id, MemberUpdateNicknameDto memberUpdateNicknameDto){
        Member member = memberRepository.findById(id).orElseThrow(()->new MyException(MyErrorCode.USER_NOT_FOUND));
        if(memberUpdateNicknameDto.getNickname()!=null) {
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

    public TokenDto login(Member member){
        LocalDateTime localDateTime = LocalDateTime.now();
        long tokenValidMillisecond = 1000L * 60 * 60 * 2 ;//2시간
        long refreshValidMillisecond = 1000L * 60 *60 *24;//24시간
        String accessToken = tokenProvider.createToken(member.getId().toString(),member.getRoles(),localDateTime);
        String refreshToken = tokenProvider.createRefreshToken(member.getId().toString(),localDateTime);
        return TokenDto.of(accessToken,refreshToken,localDateTime.plus(Duration.ofMillis(tokenValidMillisecond)).toString(),localDateTime.plus(Duration.ofMillis(refreshValidMillisecond)).toString());
    }

    public TokenDto refreshToken(String token){
        if(!tokenProvider.validateRefreshToken(token)){
            throw new MyException(MyErrorCode.EXPIRED_TOKEN);
        }
        Long id = Long.valueOf(tokenProvider.getUsernameByRefresh(token));
        Member member = memberRepository.findById(id).orElseThrow(()->new MyException(MyErrorCode.USER_NOT_FOUND));
        return login(member);
    }


    public MemberResponseDto getMember(Member member){
        if(member.getRoles().contains("ROLE_ADMIN")){
            return MemberResponseDto.adminMember(member);
        }
        else {
            return MemberResponseDto.userMember(member);
        }
    }

    public List<MemberResponseDto> getAllMember(){
        return memberRepository.findAll().stream().map(this::getMember).collect(Collectors.toList());
    }

    @Transactional
    public TokenDto schoolLogin(LoginDto loginDto){
        if (!schoolLoginRepository.loginCheck(loginDto.getStudentId(), loginDto.getPassword())) {
            throw new MyException(MyErrorCode.STUDENT_LOGIN_ERROR);
        }
        if (!memberRepository.existsByStudentId(loginDto.getStudentId())) {
            createMember(loginDto.getStudentId());
        }
        Member member = memberRepository.findByStudentId(loginDto.getStudentId()).orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        return login(member);

    }

    public void createMember(String studentId){
        Member member = Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build();
        memberRepository.save(member);
    }

}
