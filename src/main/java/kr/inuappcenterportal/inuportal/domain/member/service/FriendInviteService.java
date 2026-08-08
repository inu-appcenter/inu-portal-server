package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmAsyncService;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendInviteCodeResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendInvitePreviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FriendResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.FriendInviteCode;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendInviteCodeRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 친구추가 URL/QR 초대 코드 발급 및 수락.
 *
 * <p>링크 소유자는 링크를 만든 시점에 이미 친구 추가에 동의한 것으로 보므로, 링크를 통해 들어온
 * 상대는 별도 승인 없이 곧바로 {@link FriendStatus#ACCEPTED} 상태의 친구가 된다.
 * 그 대신 링크가 유출됐을 때를 대비해 사용자가 직접 재발급(=기존 링크 폐기)할 수 있다.
 */
@Service
@RequiredArgsConstructor
public class FriendInviteService {

    /** URL/QR에 그대로 실리므로 대소문자 혼동이 적고 인코딩이 필요 없는 문자만 사용한다. */
    private static final String CODE_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 22;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final FriendInviteCodeRepository friendInviteCodeRepository;
    private final FriendRepository friendRepository;
    private final MemberRepository memberRepository;
    private final BlockRepository blockRepository;
    private final FcmAsyncService fcmAsyncService;
    private final Clock clock;

    @Value("${friendInviteBaseUrl:https://intip.inuappcenter.kr}")
    private String friendInviteBaseUrl;

    /** 유효한 코드가 있으면 그대로, 없으면 새로 발급해서 돌려준다. */
    @Transactional
    public FriendInviteCodeResponseDto getOrCreateInviteCode(Long memberId) {
        Member member = findMember(memberId);

        FriendInviteCode inviteCode = friendInviteCodeRepository
                .findFirstByMemberAndRevokedAtIsNullOrderByIdDesc(member)
                .orElseGet(() -> issueCode(member));

        return toResponse(inviteCode);
    }

    /** 기존 코드를 모두 폐기하고 새 코드를 발급한다. 유출된 링크를 끊는 유일한 수단이다. */
    @Transactional
    public FriendInviteCodeResponseDto refreshInviteCode(Long memberId) {
        Member member = findMember(memberId);

        LocalDateTime now = LocalDateTime.now(clock);
        List<FriendInviteCode> activeCodes = friendInviteCodeRepository.findAllByMemberAndRevokedAtIsNull(member);
        activeCodes.forEach(activeCode -> activeCode.revoke(now));

        return toResponse(issueCode(member));
    }

    /**
     * 초대 링크를 연 사람에게 보여줄 링크 소유자 정보.
     * 비로그인 상태에서도 "누구의 링크인지" 확인하고 로그인 여부를 결정할 수 있어야 하므로 인증을 요구하지 않는다.
     */
    @Transactional(readOnly = true)
    public FriendInvitePreviewResponseDto getInvitePreview(String code) {
        Member owner = findActiveCode(code).getMember();

        return FriendInvitePreviewResponseDto.builder()
                .nickname(owner.getNickname())
                .studentId(owner.getMaskedStudentId())
                .fireId(owner.getFireId())
                .build();
    }

    /** 초대 코드를 수락해 즉시 친구가 된다. */
    @Transactional
    public FriendResponseDto acceptInvite(Long memberId, String code) {
        Member accepter = findMember(memberId);
        Member owner = findActiveCode(code).getMember();

        if (accepter.getId().equals(owner.getId())) {
            throw new MyException(MyErrorCode.NOT_SELF_FRIEND_REQUEST);
        }

        if (blockRepository.existsByBlockerAndBlocked(accepter, owner) ||
                blockRepository.existsByBlockerAndBlocked(owner, accepter)) {
            throw new MyException(MyErrorCode.USER_NOT_FOUND);
        }

        Friend friend = findExistingFriendship(accepter, owner)
                .map(existing -> {
                    // 이미 친구인 상태에서 링크를 또 열었을 때. 새로 맺을 것이 없으니 알림도 보내지 않는다.
                    if (existing.getStatus() == FriendStatus.ACCEPTED) {
                        throw new MyException(MyErrorCode.ALREADY_FRIEND_OR_REQUESTED);
                    }
                    // 이미 대기 중이던 요청이 있으면 링크 수락으로 갈음한다.
                    existing.accept();
                    return existing;
                })
                .orElseGet(() -> friendRepository.save(Friend.builder()
                        .requester(accepter)
                        .receiver(owner)
                        .status(FriendStatus.ACCEPTED)
                        .build()));

        fcmAsyncService.sendAsyncTrackedNotification(
                List.of(owner.getId()),
                "친구 추가",
                accepter.getNickname() + "님과 친구가 되었습니다.",
                FcmMessageType.FRIEND,
                friend.getId(),
                "/friend/list");

        return FriendResponseDto.builder()
                .friendId(friend.getId())
                .friendMemberId(owner.getId())
                .nickname(owner.getNickname())
                .studentId(owner.getMaskedStudentId())
                .fireId(owner.getFireId())
                .build();
    }

    private Optional<Friend> findExistingFriendship(Member accepter, Member owner) {
        return friendRepository.findByRequesterAndReceiver(accepter, owner)
                .or(() -> friendRepository.findByRequesterAndReceiver(owner, accepter));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
    }

    private FriendInviteCode findActiveCode(String code) {
        return friendInviteCodeRepository.findByCodeAndRevokedAtIsNull(code)
                .orElseThrow(() -> new MyException(MyErrorCode.NOT_FOUND_FRIEND_INVITE_CODE));
    }

    private FriendInviteCode issueCode(Member member) {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String candidate = generateCode();
            if (!friendInviteCodeRepository.existsByCode(candidate)) {
                return friendInviteCodeRepository.save(FriendInviteCode.builder()
                        .member(member)
                        .code(candidate)
                        .build());
            }
        }
        throw new MyException(MyErrorCode.FAIL_CREATE_FRIEND_INVITE_CODE);
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private FriendInviteCodeResponseDto toResponse(FriendInviteCode inviteCode) {
        return FriendInviteCodeResponseDto.builder()
                .code(inviteCode.getCode())
                .url(friendInviteBaseUrl + "/friend/invite/" + inviteCode.getCode())
                .build();
    }
}
