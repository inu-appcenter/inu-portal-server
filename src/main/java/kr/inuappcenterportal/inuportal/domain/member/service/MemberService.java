package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.dto.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.service.DailyBriefService;
import kr.inuappcenterportal.inuportal.domain.department.repository.SchoolDepartmentRepository;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private static final long LAST_SEEN_UPDATE_THRESHOLD_MINUTES = 5L;

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final SchoolLoginRepository schoolLoginRepository;
    private final FriendRepository friendRepository;
    private final BlockRepository blockRepository;
    private final SchoolDepartmentRepository schoolDepartmentRepository;
    private final DailyBriefService dailyBriefService;

    private static final List<String> FORBIDDEN_NICKNAME_KEYWORDS = List.of(
            "알림", "공지", "알람", "운영자", "운영진", "관리자", "시스템", "스태프", "어드민",
            "notice", "admin", "system", "staff", "intip", "인팁", "appcenter", "앱센터"
    );

    private void validateNicknameKeywords(String nickname) {
        if (nickname == null) return;
        String normalized = nickname.toLowerCase().replaceAll("\\s+", "");
        for (String keyword : FORBIDDEN_NICKNAME_KEYWORDS) {
            if (normalized.contains(keyword)) {
                throw new MyException(MyErrorCode.INVALID_NICKNAME_KEYWORD);
            }
        }
    }

    @Transactional
    public Long updateMemberNicknameFireId(Long id, MemberUpdateNicknameDto memberUpdateNicknameDto) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        if (memberUpdateNicknameDto.getNickname() != null) {
            validateNicknameKeywords(memberUpdateNicknameDto.getNickname());
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
        
        Optional<Member> existingMemberOpt = memberRepository.findByStudentId(studentId);
        
        if (existingMemberOpt.isPresent()) {
            Member member = existingMemberOpt.get();
            boolean isTestUser = member.getRoles().stream()
                    .anyMatch(role -> role.endsWith("_TEST"));
            if (isTestUser) {
                if (!studentId.equals(loginDto.getPassword())) {
                    throw new MyException(MyErrorCode.STUDENT_LOGIN_ERROR);
                }
                return login(member);
            }
        }

        if (!schoolLoginRepository.loginCheck(studentId, loginDto.getPassword())) {
            throw new MyException(MyErrorCode.STUDENT_LOGIN_ERROR);
        }

        List<String> roles = schoolLoginRepository.resolveRoles(studentId);
        Member member = existingMemberOpt
                .map(existingMember -> synchronizeRoles(existingMember, roles))
                .orElseGet(() -> createMember(studentId, roles));
        return login(member);
    }

    public void createMember(String studentId) {
        createMember(studentId, Collections.singletonList("ROLE_USER"));
    }

    private Member createMember(String studentId, List<String> roles) {
        Member member = Member.builder().studentId(studentId).roles(roles).build();
        Member savedMember = memberRepository.save(member);
        dailyBriefService.createDefaultSetting(savedMember);
        return savedMember;
    }

    private Member synchronizeRoles(Member member, List<String> roles) {
        if (shouldPreserveAdminRole(member, roles)) {
            return member;
        }

        if (!member.getRoles().equals(roles)) {
            member.updateRoles(roles);
            return memberRepository.save(member);
        }
        return member;
    }

    private boolean shouldPreserveAdminRole(Member member, List<String> roles) {
        return member.getRoles() != null
                && member.getRoles().contains("ROLE_ADMIN")
                && (roles == null || !roles.contains("ROLE_ADMIN"));
    }

    @Transactional
    public MemberResponseDto updateMemberDepartment(Long memberId, Department department) {
        Member member = findMemberById(memberId);
        member.updateDepartment(department);
        return getMemberResponseDto(member);
    }

    @Transactional
    public MemberResponseDto updateSchoolDepartment(Long memberId, String departmentCode) {
        Member member = findMemberById(memberId);
        var department = schoolDepartmentRepository.findByCodeAndActiveTrue(departmentCode)
                .orElseThrow(() -> new MyException(MyErrorCode.SCHOOL_DEPARTMENT_NOT_FOUND));
        member.updateSchoolDepartment(department);
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



    @Transactional
    public boolean toggleChatPush(Long memberId) {
        Member member = findMemberById(memberId);
        member.toggleChatPush();
        return member.getChatPushEnabled();
    }

    @Transactional
    public void updateNearbyVisibility(Long memberId, Boolean enabled) {
        Member member = findMemberById(memberId);
        member.updateNearbyVisibility(enabled);
    }

    @Transactional
    public void updateLocation(Long memberId, LocationUpdateRequestDto requestDto) {
        if (requestDto == null || requestDto.getLatitude() == null || requestDto.getLongitude() == null) {
            throw new MyException(MyErrorCode.REQUIRED_LOCATION_PARAMETER);
        }
        if (requestDto.getLatitude() < -90.0 || requestDto.getLatitude() > 90.0 ||
                requestDto.getLongitude() < -180.0 || requestDto.getLongitude() > 180.0) {
            throw new MyException(MyErrorCode.INVALID_LOCATION_VALUE);
        }
        Member member = findMemberById(memberId);
        member.updateLocation(requestDto.getLatitude(), requestDto.getLongitude());
    }
}
