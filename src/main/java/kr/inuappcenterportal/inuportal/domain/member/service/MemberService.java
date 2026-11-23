package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.member.dto.*;
import kr.inuappcenterportal.inuportal.domain.member.enums.RealNameSource;
import kr.inuappcenterportal.inuportal.domain.department.model.SchoolDepartment;
import kr.inuappcenterportal.inuportal.domain.member.enums.MemberType;
import kr.inuappcenterportal.inuportal.domain.member.dto.FreshmanLoginDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.OnboardingRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MergeFreshmanRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MergeFreshmanResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.SyncAcademicRequestDto;
import kr.inuappcenterportal.inuportal.domain.post.repository.PostRepository;
import kr.inuappcenterportal.inuportal.domain.reply.repository.ReplyRepository;
import kr.inuappcenterportal.inuportal.domain.scrap.repository.ScrapRepository;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.model.MemberNameConsent;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberNameConsentRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.repository.DailyBriefSettingRepository;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.service.DailyBriefService;
import kr.inuappcenterportal.inuportal.domain.department.repository.SchoolDepartmentRepository;
import kr.inuappcenterportal.inuportal.global.config.FreshmanSecurityProperties;
import kr.inuappcenterportal.inuportal.global.config.TokenProvider;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.security.FirebaseAppCheckVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private static final long LAST_SEEN_UPDATE_THRESHOLD_MINUTES = 5L;
    // 실명: 2~30자, 한글·영문·공백
    private static final Pattern REAL_NAME_PATTERN = Pattern.compile("[가-힣a-zA-Z ]{2,30}");
    private static final String DEFAULT_DEV_VERIFY_SECRET = "intip-dev-freshman-verify-secret-change-me";

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final SchoolLoginRepository schoolLoginRepository;
    private final FriendRepository friendRepository;
    private final BlockRepository blockRepository;
    private final SchoolDepartmentRepository schoolDepartmentRepository;
    private final DailyBriefService dailyBriefService;
    private final MemberNameConsentRepository memberNameConsentRepository;
    private final PostRepository postRepository;
    private final ReplyRepository replyRepository;
    private final ScrapRepository scrapRepository;
    private final DailyBriefSettingRepository dailyBriefSettingRepository;
    private final FirebaseAppCheckVerifier firebaseAppCheckVerifier;
    private final FreshmanRateLimiter freshmanRateLimiter;
    private final FreshmanSecurityProperties freshmanSecurityProperties;
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("^\\d{9}$");

    private static final List<String> FORBIDDEN_NICKNAME_KEYWORDS = List.of(
            "알림", "공지", "알람", "운영자", "운영진", "관리자", "시스템", "스태프", "어드민",
            "notice", "admin", "system", "staff", "intip", "인팁", "appcenter", "앱센터",
            "탈퇴한사용자"
    );

    // 실명은 시간 맞추기·친구 화면에 닉네임 대신 보인다. 운영진인 척하는 이름만 막는다.
    // 닉네임 금지어 중 '공지'·'알림'·'알람'은 실제 이름(예: 공지영)에 들어갈 수 있어 뺐다.
    private static final List<String> FORBIDDEN_REAL_NAME_KEYWORDS = List.of(
            "운영자", "운영진", "관리자", "시스템", "스태프", "어드민",
            "admin", "system", "staff", "intip", "인팁", "appcenter", "앱센터"
    );

    private void validateRealNameKeywords(String realName) {
        String normalized = realName.toLowerCase().replaceAll("\\s+", "");
        for (String keyword : FORBIDDEN_REAL_NAME_KEYWORDS) {
            if (normalized.contains(keyword)) {
                throw new MyException(MyErrorCode.INVALID_REAL_NAME);
            }
        }
    }

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
        boolean hasRealName = memberUpdateNicknameDto.getRealName() != null;
        if (memberUpdateNicknameDto.getNickname() == null && memberUpdateNicknameDto.getFireId() == null) {
            if (!hasRealName) {
                throw new MyException(MyErrorCode.EMPTY_REQUEST);
            }
        } else if (memberUpdateNicknameDto.getNickname() != null) {
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
        } else {
            member.updateFire(memberUpdateNicknameDto.getFireId());
        }
        if (hasRealName) {
            updateRealName(member, memberUpdateNicknameDto);
        }
        return member.getId();
    }

    // 실명: ""이면 삭제(포털 동의가 있었으면 철회 기록), 아니면 2~30자 한글·영문·공백.
    // PORTAL_SCRAPE면 약관 버전이 필요하고 동의 기록을 시각과 함께 남긴다.
    private void updateRealName(Member member, MemberUpdateNicknameDto dto) {
        LocalDateTime now = LocalDateTime.now();
        if (dto.getRealName().isEmpty()) {
            if (member.getRealNameSource() == RealNameSource.PORTAL_SCRAPE) {
                memberNameConsentRepository.save(MemberNameConsent.withdraw(member.getId(), now));
            }
            member.clearRealName();
            return;
        }
        String realName = dto.getRealName().trim();
        if (!REAL_NAME_PATTERN.matcher(realName).matches()) {
            throw new MyException(MyErrorCode.INVALID_REAL_NAME);
        }
        validateRealNameKeywords(realName);
        if (dto.getRealNameSource() == null) {
            throw new MyException(MyErrorCode.REAL_NAME_SOURCE_REQUIRED);
        }
        // 포털에서 가져온 이름을 직접 입력한 이름으로 바꾸면 포털 동의는 더 쓰이지 않는다. 철회로 기록한다.
        if (member.getRealNameSource() == RealNameSource.PORTAL_SCRAPE && dto.getRealNameSource() == RealNameSource.MANUAL) {
            memberNameConsentRepository.save(MemberNameConsent.withdraw(member.getId(), now));
        }
        if (dto.getRealNameSource() == RealNameSource.PORTAL_SCRAPE) {
            if (dto.getConsentTermsVersion() == null || dto.getConsentTermsVersion().isBlank()) {
                throw new MyException(MyErrorCode.CONSENT_TERMS_VERSION_REQUIRED);
            }
            memberNameConsentRepository.save(MemberNameConsent.agree(member.getId(), dto.getConsentTermsVersion(), now));
        }
        member.updateRealName(realName, dto.getRealNameSource());
    }

    @Transactional
    public TokenDto login(Member member) {
        return login(member, false);
    }

    private TokenDto login(Member member, boolean newMember) {
        LocalDateTime localDateTime = LocalDateTime.now();
        String accessToken = tokenProvider.createToken(member.getId().toString(), member.getRoles(), localDateTime);
        String refreshToken = tokenProvider.createRefreshToken(member.getId().toString(), localDateTime);
        return TokenDto.of(
                accessToken,
                refreshToken,
                tokenProvider.getAccessTokenExpiry(localDateTime).toString(),
                tokenProvider.getRefreshTokenExpiry(localDateTime).toString(),
                newMember
        );
    }

    public TokenDto refreshToken(String token) {
        if (!tokenProvider.validateRefreshToken(token)) {
            throw new MyException(MyErrorCode.EXPIRED_TOKEN);
        }
        Long id = Long.valueOf(tokenProvider.getUsernameByRefresh(token));
        Member member = memberRepository.findById(id).orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        if (member.isWithdrawn()) {
            throw new MyException(MyErrorCode.WITHDRAWN_MEMBER);
        }
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
        return memberRepository.findAllActive().stream().map(this::getMemberResponseDto).collect(Collectors.toList());
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
        return login(member, existingMemberOpt.isEmpty());
    }

    public void createMember(String studentId) {
        createMember(studentId, Collections.singletonList("ROLE_USER"));
    }

    public MemberType resolveMemberType(String studentId) {
        if (studentId != null && STUDENT_ID_PATTERN.matcher(studentId).matches()) {
            return MemberType.STUDENT;
        }
        return MemberType.FACULTY;
    }

    private Member createMember(String studentId, List<String> roles) {
        MemberType type = resolveMemberType(studentId);
        Member member = Member.builder().studentId(studentId).roles(roles).build();
        member.setMemberType(type);
        if (type == MemberType.FACULTY) {
            member.setIsAcademicVerified(true);
        }
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
    public boolean updateChatPush(Long memberId, boolean enabled) {
        Member member = findMemberById(memberId);
        member.setChatPushEnabled(enabled);
        return Boolean.TRUE.equals(member.getChatPushEnabled());
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

    @Transactional
    public TokenDto freshmanLogin(FreshmanLoginDto dto, String appCheckToken, String clientIp) {
        String examNo = dto.getExamNo().trim();
        freshmanRateLimiter.checkAttempt(clientIp, examNo);
        firebaseAppCheckVerifier.verify(appCheckToken);

        String verifyHash = computeFreshmanVerifyHash(dto.getName(), dto.getBirthDate());
        LocalDateTime now = LocalDateTime.now();

        Optional<Member> existingOpt = memberRepository.findByExamNo(examNo);
        if (existingOpt.isPresent()) {
            Member existing = existingOpt.get();
            if (existing.getMemberType() != MemberType.FRESHMAN) {
                throw new MyException(MyErrorCode.FRESHMAN_AUTH_MISMATCH);
            }
            if (existing.isFreshmanExpired(now)) {
                throw new MyException(MyErrorCode.FRESHMAN_EXPIRED);
            }
            if (!Objects.equals(verifyHash, existing.getFreshmanVerifyHash())) {
                freshmanRateLimiter.recordExamFailure(examNo);
                throw new MyException(MyErrorCode.FRESHMAN_AUTH_MISMATCH);
            }
            freshmanRateLimiter.clearExamFailure(examNo);
            if (existing.getSchoolDepartment() == null && dto.getDeptNm() != null && !dto.getDeptNm().isBlank()) {
                SchoolDepartment resolvedDept = resolveSchoolDepartment(dto.getDeptNm());
                if (resolvedDept != null) {
                    existing.updateSchoolDepartment(resolvedDept);
                }
            }
            return login(existing);
        }

        freshmanRateLimiter.checkSignupLimit(clientIp);
        SchoolDepartment schoolDept = resolveSchoolDepartment(dto.getDeptNm());

        Member newFreshman = Member.builder()
                .studentId("F_" + examNo)
                .roles(List.of("ROLE_USER"))
                .build();
        newFreshman.setMemberType(MemberType.FRESHMAN);
        newFreshman.setExamNo(examNo);
        newFreshman.setFreshmanVerifyHash(verifyHash);
        newFreshman.setFreshmanExpiresAt(Member.calculateFreshmanExpiresAt(now));
        newFreshman.updateRealName(dto.getName().trim(), RealNameSource.PORTAL_SCRAPE);
        newFreshman.updateSchoolDepartment(schoolDept);
        newFreshman.setIsAcademicVerified(true);

        Member saved = memberRepository.save(newFreshman);
        if (dailyBriefService != null) {
            dailyBriefService.createDefaultSetting(saved);
        }
        return login(saved, true);
    }

    @Transactional
    public MemberResponseDto completeOnboarding(Long memberId, OnboardingRequestDto dto) {
        Member member = findMemberById(memberId);

        // 닉네임 유효성 검사
        String nickname = dto.getNickname().trim();
        if (nickname.isEmpty()) {
            throw new MyException(MyErrorCode.NOT_BLANK_NICKNAME);
        }
        validateNicknameKeywords(nickname);
        if (STUDENT_ID_PATTERN.matcher(nickname).matches() || nickname.equals(member.getStudentId())) {
            throw new MyException(MyErrorCode.INVALID_NICKNAME_KEYWORD);
        }
        if (!nickname.equals(member.getNickname()) && memberRepository.existsByNickname(nickname)) {
            throw new MyException(MyErrorCode.USER_DUPLICATE_NICKNAME);
        }

        // 실명 처리
        String realName = dto.getRealName() != null ? dto.getRealName().trim() : null;
        RealNameSource source = dto.getRealNameSource();
        if (realName != null && !realName.isEmpty()) {
            if (!REAL_NAME_PATTERN.matcher(realName).matches()) {
                throw new MyException(MyErrorCode.INVALID_REAL_NAME);
            }
            validateRealNameKeywords(realName);
            if (source == null) {
                source = RealNameSource.PORTAL_SCRAPE;
            }
        }

        // 학과 처리 (코드 우선, 학과명 폴백 허용)
        SchoolDepartment schoolDept = resolveSchoolDepartment(dto.getDepartmentCode());
        if (schoolDept == null) {
            schoolDept = member.getSchoolDepartment();
        }

        boolean isVerified = Boolean.TRUE.equals(dto.getIsAcademicVerified()) && source == RealNameSource.PORTAL_SCRAPE;
        if (member.getMemberType() == MemberType.FACULTY) {
            isVerified = true;
        }

        member.completeOnboarding(nickname, dto.getFireId(), realName, source, schoolDept, isVerified);
        return getMemberResponseDto(member);
    }

    @Transactional
    public MergeFreshmanResponseDto checkAndMergeFreshman(Long currentMemberId, MergeFreshmanRequestDto dto) {
        Member currentMember = findMemberById(currentMemberId);
        if (currentMember.getMemberType() != MemberType.STUDENT) {
            return MergeFreshmanResponseDto.notMerged();
        }

        String verifyHash = computeFreshmanVerifyHash(dto.getKoreanName(), dto.getBirthDate());
        Optional<Member> freshmanOpt = memberRepository.findFreshmanByVerifyHash(verifyHash);

        if (freshmanOpt.isEmpty()) {
            return MergeFreshmanResponseDto.notMerged();
        }

        Member freshman = freshmanOpt.get();
        if (freshman.getId().equals(currentMember.getId())) {
            return MergeFreshmanResponseDto.notMerged();
        }

        SchoolDepartment resolvedDept = resolveSchoolDepartment(dto.getDepartmentName());
        boolean hasExistingActivity = postRepository.existsByMember(currentMember)
                || replyRepository.existsByMember(currentMember)
                || scrapRepository.existsByMember(currentMember);

        if (!hasExistingActivity) {
            // 빈 신규 STUDENT 레코드를 제거하고, 기존 FRESHMAN 레코드를 정규 학번으로 승격하여 모든 FK 데이터를 보존한다.
            String officialStudentId = currentMember.getStudentId();
            if (dailyBriefSettingRepository != null) {
                dailyBriefSettingRepository.deleteByMember(currentMember);
                dailyBriefSettingRepository.flush();
            }
            memberRepository.delete(currentMember);
            memberRepository.flush();

            freshman.upgradeToStudent(officialStudentId, dto.getKoreanName().trim(), resolvedDept);
            TokenDto upgradedToken = login(freshman);
            return MergeFreshmanResponseDto.merged(freshman.getExamNo(), upgradedToken);
        }

        log.warn("[MergeFreshman] currentMember({}) already has activity data; transferring posts/replies/scraps instead of in-place upgrade.", currentMember.getId());
        postRepository.transferMember(freshman, currentMember);
        replyRepository.transferMember(freshman, currentMember);
        scrapRepository.transferMember(freshman, currentMember);

        currentMember.updateNicknameAndFire(freshman.getNickname(), freshman.getFireId());
        currentMember.upgradeFromFreshman(currentMember.getStudentId());
        if (resolvedDept != null) {
            currentMember.updateSchoolDepartment(resolvedDept);
        }

        if (dailyBriefSettingRepository != null) {
            dailyBriefSettingRepository.deleteByMember(freshman);
            dailyBriefSettingRepository.flush();
        }
        memberRepository.delete(freshman);
        return MergeFreshmanResponseDto.merged(freshman.getExamNo(), login(currentMember));
    }

    @Transactional
    public MemberResponseDto syncOfficialAcademic(Long memberId, SyncAcademicRequestDto dto) {
        Member member = findMemberById(memberId);
        String realName = dto.getRealName().trim();
        if (!REAL_NAME_PATTERN.matcher(realName).matches()) {
            throw new MyException(MyErrorCode.INVALID_REAL_NAME);
        }
        validateRealNameKeywords(realName);

        SchoolDepartment schoolDept = resolveSchoolDepartment(dto.getDepartmentCode());
        if (schoolDept == null) {
            schoolDept = member.getSchoolDepartment();
        }

        member.syncOfficialAcademic(realName, schoolDept);
        return getMemberResponseDto(member);
    }

    @Transactional
    public TokenDto convertFreshmanToStudent(Long memberId, LoginDto loginDto) {
        Member freshman = findMemberById(memberId);
        if (freshman.getMemberType() != MemberType.FRESHMAN) {
            throw new MyException(MyErrorCode.FRESHMAN_CONVERT_NOT_ALLOWED);
        }

        String studentId = loginDto.getStudentId().trim();
        if (!schoolLoginRepository.loginCheck(studentId, loginDto.getPassword())) {
            throw new MyException(MyErrorCode.STUDENT_LOGIN_ERROR);
        }

        List<String> roles = schoolLoginRepository.resolveRoles(studentId);
        Optional<Member> existingOpt = memberRepository.findByStudentId(studentId);

        if (existingOpt.isPresent() && !existingOpt.get().getId().equals(freshman.getId())) {
            Member existingStudent = existingOpt.get();
            boolean hasExistingActivity = postRepository.existsByMember(existingStudent)
                    || replyRepository.existsByMember(existingStudent)
                    || scrapRepository.existsByMember(existingStudent);

            if (hasExistingActivity) {
                log.warn("[ConvertFreshman] existingStudent({}) already has activity data; transferring freshman({}) data.", existingStudent.getId(), freshman.getId());
                postRepository.transferMember(freshman, existingStudent);
                replyRepository.transferMember(freshman, existingStudent);
                scrapRepository.transferMember(freshman, existingStudent);

                if (existingStudent.getNickname() == null || existingStudent.getNickname().equals(existingStudent.getStudentId())) {
                    if (freshman.getNickname() != null && !freshman.getNickname().equals(freshman.getStudentId())) {
                        existingStudent.updateNicknameAndFire(freshman.getNickname(), freshman.getFireId());
                    }
                }
                existingStudent.upgradeToStudent(studentId, freshman.getRealName(), freshman.getSchoolDepartment());

                if (dailyBriefSettingRepository != null) {
                    dailyBriefSettingRepository.deleteByMember(freshman);
                    dailyBriefSettingRepository.flush();
                }
                memberRepository.delete(freshman);
                synchronizeRoles(existingStudent, roles);
                return login(existingStudent);
            }

            if (dailyBriefSettingRepository != null) {
                dailyBriefSettingRepository.deleteByMember(existingStudent);
                dailyBriefSettingRepository.flush();
            }
            memberRepository.delete(existingStudent);
            memberRepository.flush();
        }

        freshman.upgradeToStudent(studentId, freshman.getRealName(), freshman.getSchoolDepartment());
        synchronizeRoles(freshman, roles);
        return login(freshman);
    }

    public String computeFreshmanVerifyHash(String name, String birthDate) {
        String normalizedName = name == null ? "" : name.trim().replaceAll("\\s+", "");
        String normalizedBirth = birthDate == null ? "" : birthDate.replaceAll("\\D", "");
        String payload = normalizedName + ":" + normalizedBirth;
        String secret = freshmanSecurityProperties != null
                ? freshmanSecurityProperties.resolvedVerifySecret()
                : DEFAULT_DEV_VERIFY_SECRET;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new IllegalStateException("신입생 검증 해시 계산 실패", e);
        }
    }

    private SchoolDepartment resolveSchoolDepartment(String codeOrName) {
        if (codeOrName == null || codeOrName.isBlank()) {
            return null;
        }
        String trimmed = codeOrName.trim();
        return schoolDepartmentRepository.findByCodeAndActiveTrue(trimmed)
                .or(() -> schoolDepartmentRepository.findFirstByNameAndActiveTrue(trimmed))
                .orElse(null);
    }
}
