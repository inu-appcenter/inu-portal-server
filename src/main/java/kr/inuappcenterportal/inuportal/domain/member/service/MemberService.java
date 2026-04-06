package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.dto.LoginDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberUpdateNicknameDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private static final long LAST_SEEN_UPDATE_THRESHOLD_MINUTES = 5L;

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final SchoolLoginRepository schoolLoginRepository;

    @Transactional
    public Long updateMemberNicknameFireId(Long id, MemberUpdateNicknameDto memberUpdateNicknameDto) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        if (memberUpdateNicknameDto.getNickname() != null) {
            if (memberRepository.existsByNickname(memberUpdateNicknameDto.getNickname())) {
                throw new MyException(MyErrorCode.USER_DUPLICATE_NICKNAME);
            }
            if (memberUpdateNicknameDto.getNickname().trim().isEmpty()) {
                throw new MyException(MyErrorCode.NOT_BLANK_NICKNAME);
            }
            if (memberUpdateNicknameDto.getFireId() != null) {
                member.updateNicknameAndFire(memberUpdateNicknameDto.getNickname(), memberUpdateNicknameDto.getFireId());
            } else {
                member.updateNickName(memberUpdateNicknameDto.getNickname());
            }
        } else if (memberUpdateNicknameDto.getFireId() != null) {
            member.updateFire(memberUpdateNicknameDto.getFireId());
        } else {
            throw new MyException(MyErrorCode.EMPTY_REQUEST);
        }
        return member.getId();
    }

    @Transactional
    public void delete(Member member) {
        memberRepository.delete(member);
    }

    public TokenDto login(Member member) {
        LocalDateTime localDateTime = LocalDateTime.now();
        String accessToken = tokenProvider.createToken(member.getId().toString(), member.getRoles(), localDateTime);
        String refreshToken = tokenProvider.createRefreshToken(member.getId().toString(), localDateTime);
        return TokenDto.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpiry(localDateTime).toString(),
                tokenProvider.getRefreshTokenExpiry(localDateTime).toString()
        );
    }

    public TokenDto refreshToken(String token) {
        if (!tokenProvider.validateRefreshToken(token)) {
            throw new MyException(MyErrorCode.EXPIRED_TOKEN);
        }
        Long id = Long.valueOf(tokenProvider.getUsernameByRefresh(token));
        Member member = memberRepository.findById(id).orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        return login(member);
    }

    @Transactional
    public MemberResponseDto getCurrentMember(Member member) {
        Member persistedMember = findMemberById(member.getId());
        updateLastSeenAtIfNeeded(persistedMember);
        return getMemberResponseDto(persistedMember);
    }

    public MemberResponseDto getMember(Member member) {
        return getMemberResponseDto(member);
    }

    public List<MemberResponseDto> getAllMember() {
        return memberRepository.findAll().stream().map(this::getMemberResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public TokenDto schoolLogin(LoginDto loginDto) {
        String studentId = loginDto.getStudentId();
        if (!schoolLoginRepository.loginCheck(studentId, loginDto.getPassword())) {
            throw new MyException(MyErrorCode.STUDENT_LOGIN_ERROR);
        }

        List<String> roles = schoolLoginRepository.resolveRoles(studentId);
        Member member = memberRepository.findByStudentId(studentId)
                .map(existingMember -> synchronizeRoles(existingMember, roles))
                .orElseGet(() -> createMember(studentId, roles));
        return login(member);
    }

    public void createMember(String studentId) {
        createMember(studentId, Collections.singletonList("ROLE_USER"));
    }

    private Member createMember(String studentId, List<String> roles) {
        Member member = Member.builder().studentId(studentId).roles(roles).build();
        return memberRepository.save(member);
    }

    private Member synchronizeRoles(Member member, List<String> roles) {
        if (!member.getRoles().equals(roles)) {
            member.updateRoles(roles);
            return memberRepository.save(member);
        }
        return member;
    }

    @Transactional
    public MemberResponseDto updateMemberDepartment(Long memberId, Department department) {
        Member member = findMemberById(memberId);
        member.updateDepartment(department);
        return getMemberResponseDto(member);
    }

    @Transactional
    public MemberResponseDto agreeTerms(Long memberId) {
        Member member = findMemberById(memberId);
        member.agreeTerms();
        return getMemberResponseDto(member);
    }

    private MemberResponseDto getMemberResponseDto(Member member) {
        if (member.getRoles().contains("ROLE_ADMIN")) {
            return MemberResponseDto.adminMember(member);
        } else {
            return MemberResponseDto.userMember(member);
        }
    }

    private void updateLastSeenAtIfNeeded(Member member) {
        LocalDateTime now = LocalDateTime.now();
        if (member.shouldUpdateLastSeenAt(now, LAST_SEEN_UPDATE_THRESHOLD_MINUTES)) {
            member.updateLastSeenAt();
        }
    }

    private Member findMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
    }
}
