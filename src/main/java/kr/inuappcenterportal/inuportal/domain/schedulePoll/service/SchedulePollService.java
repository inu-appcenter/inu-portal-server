package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatMemberStatus;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomMemberRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollColumnResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollCreateResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollDetailResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollJoinPreviewResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollRevisionResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.DurationMode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.JoinBlockReason;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ParticipantStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.PollDay;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollErrorCode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollException;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SchedulePollService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final String DEFAULT_TITLE = "회의 시간 맞추기";
    private static final int MIN_RANGE_START_MINUTES = 9 * 60;
    private static final int MAX_ONE_TIME_DAYS = 14;
    private static final Set<Integer> FIXED_DURATIONS = Set.of(30, 60, 90);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollSubmissionRepository schedulePollSubmissionRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final BlockRepository blockRepository;
    private final Clock clock;

    @Value("${schedulePollBaseUrl:https://intip.inuappcenter.kr}")
    private String baseUrl;

    // 1. 조율 생성
    // 형식 오류는 요청 DTO 검증(VALIDATION_ERROR), 규칙 위반은 여기서 INVALID_RANGE / NOT_SHARE_TARGET
    @Transactional
    public SchedulePollCreateResponseDto create(Member member, SchedulePollCreateRequestDto request) {
        LocalDateTime now = LocalDateTime.now(clock);
        validateDuration(request.durationMode(), request.durationMin());

        // 정기 / 일회성 규칙
        if (request.isRecurring()) {
            validateDays(request.days());
        } else {
            validateDates(request.startDate(), request.endDate(), now.toLocalDate());
        }
        // 시간대: 30분 단위, 09:00 ≤ 시작 < 끝 ≤ 24:00, 회의 길이 이상
        int rangeStart = toRangeMinutes(request.rangeStart());
        int rangeEnd = toRangeMinutes(request.rangeEnd());
        if (rangeStart < MIN_RANGE_START_MINUTES || rangeStart >= rangeEnd
                || rangeEnd - rangeStart < request.durationMin()) {
            throw new SchedulePollException(SchedulePollErrorCode.INVALID_RANGE);
        }
        // 마감: 현재 이후
        LocalDateTime deadlineAt = request.deadlineAt() == null ? null : toSeoul(request.deadlineAt());
        if (deadlineAt != null && !deadlineAt.isAfter(now)) {
            throw new SchedulePollException(SchedulePollErrorCode.INVALID_RANGE);
        }
        // 채팅방: 내가 참여 중인 방
        if (request.originChatRoomId() != null && !chatRoomMemberRepository.existsByChatRoomIdAndMemberIdAndStatus(
                request.originChatRoomId(), member.getId(), ChatMemberStatus.JOINED)) {
            throw new SchedulePollException(SchedulePollErrorCode.NOT_SHARE_TARGET);
        }

        String title = request.title() == null ? DEFAULT_TITLE : request.title();
        String linkToken = generateLinkToken();
        SchedulePoll poll = request.isRecurring()
                ? SchedulePoll.createRecurring(member, title, request.days(), rangeStart, rangeEnd,
                request.durationMode(), request.durationMin(), deadlineAt, request.originChatRoomId(), linkToken)
                : SchedulePoll.createOneTime(member, title, request.startDate(), request.endDate(), rangeStart, rangeEnd,
                request.durationMode(), request.durationMin(), deadlineAt, request.originChatRoomId(), linkToken);
        schedulePollRepository.save(poll);

        return new SchedulePollCreateResponseDto(poll.getId(), linkToken, linkUrl(linkToken), poll.getRevision());
    }

    // 2. 조율 조회. 로그인한 사람 누구나 볼 수 있다. 다른 사람의 입력과 히트맵은 넣지 않는다.
    @Transactional(readOnly = true)
    public SchedulePollDetailResponseDto get(Member member, Long pollId) {
        SchedulePoll poll = findPoll(pollId);
        LocalDateTime now = LocalDateTime.now(clock);
        Member creator = poll.getCreator();
        boolean submitted = schedulePollSubmissionRepository.existsByPollIdAndMemberId(pollId, member.getId());

        return new SchedulePollDetailResponseDto(
                poll.getId(),
                poll.getTitle(),
                displayStatus(poll.getStatus(), poll.getDeadlineAt(), now),
                poll.isRecurring(),
                columns(poll),
                SchedulePollSlotUtils.formatMinutes(poll.getRangeStartMinutes()),
                SchedulePollSlotUtils.formatMinutes(poll.getRangeEndMinutes()),
                poll.getDurationMode(),
                poll.getDurationMinutes(),
                toOffset(poll.getDeadlineAt()),
                new SchedulePollDetailResponseDto.Creator(creator.getId(), displayName(creator)),
                creator.getId().equals(member.getId()),
                submitted ? ParticipantStatus.SUBMITTED : null,
                poll.getParticipantCount(),
                SchedulePoll.MAX_PARTICIPANTS,
                linkUrl(poll.getLinkToken()),
                poll.getRevision(),
                poll.getResultVersion(),
                toOffset(poll.isDeadlinePassed(now) ? poll.getDeadlineAt() : poll.getClosedAt()));
    }

    // 3. 변경 확인(폴링). 조회 한 번으로 끝낸다.
    @Transactional(readOnly = true)
    public SchedulePollRevisionResponseDto getRevision(Member member, Long pollId) {
        SchedulePollRepository.RevisionView view = schedulePollRepository.findRevisionView(pollId, member.getId())
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);

        return new SchedulePollRevisionResponseDto(
                view.getRevision(),
                displayStatus(view.getStatus(), view.getDeadlineAt(), now),
                view.getResultVersion(),
                view.getParticipantCount(),
                view.getSubmittedCount() > 0 ? ParticipantStatus.SUBMITTED : null,
                toOffset(view.getDeadlineAt()));
    }

    // 4. 링크 미리보기. 비로그인(member == null)도 호출할 수 있고, 참여 처리는 하지 않는다.
    // canEnter는 화면 안내용이며 최종 판정은 제출 API에서 한다.
    @Transactional(readOnly = true)
    public SchedulePollJoinPreviewResponseDto previewByLink(Member member, String token) {
        SchedulePoll poll = schedulePollRepository.findByLinkToken(token)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now(clock);
        Member creator = poll.getCreator();
        boolean submitted = member != null
                && schedulePollSubmissionRepository.existsByPollIdAndMemberId(poll.getId(), member.getId());

        JoinBlockReason blockReason = joinBlockReason(poll, member, creator, submitted, now);
        return new SchedulePollJoinPreviewResponseDto(
                poll.getId(),
                poll.getTitle(),
                displayName(creator),
                displayStatus(poll.getStatus(), poll.getDeadlineAt(), now),
                poll.getParticipantCount(),
                SchedulePoll.MAX_PARTICIPANTS,
                toOffset(poll.getDeadlineAt()),
                blockReason == null,
                blockReason,
                submitted ? ParticipantStatus.SUBMITTED : null);
    }

    // 끝남·마감 → 비로그인 → 차단 → 이미 참여자 → 10명 순서로 판정한다. null이면 입력 가능
    private JoinBlockReason joinBlockReason(SchedulePoll poll, Member member, Member creator,
                                            boolean submitted, LocalDateTime now) {
        if (poll.isClosed() || poll.isDeadlinePassed(now)) {
            return JoinBlockReason.CLOSED;
        }
        if (member == null) {
            return null;
        }
        if (isBlockedEitherWay(member, creator)) {
            return JoinBlockReason.BLOCKED;
        }
        if (submitted) {
            return null;
        }
        if (poll.getParticipantCount() >= SchedulePoll.MAX_PARTICIPANTS) {
            return JoinBlockReason.FULL;
        }
        return null;
    }

    // 내가 개설자를 차단했거나 개설자가 나를 차단한 경우
    private boolean isBlockedEitherWay(Member member, Member creator) {
        if (member.getId().equals(creator.getId())) {
            return false;
        }
        return blockRepository.existsByBlockerAndBlocked(member, creator)
                || blockRepository.existsByBlockerAndBlocked(creator, member);
    }

    private SchedulePoll findPoll(Long pollId) {
        return schedulePollRepository.findById(pollId)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));
    }

    // 마감 시각이 지났으면 스케줄러가 처리하기 전(최대 1분)이라도 CLOSED로 보여 준다.
    private SchedulePollStatus displayStatus(SchedulePollStatus status, LocalDateTime deadlineAt, LocalDateTime now) {
        if (status != SchedulePollStatus.CLOSED && deadlineAt != null && !now.isBefore(deadlineAt)) {
            return SchedulePollStatus.CLOSED;
        }
        return status;
    }

    // 정기면 요일(월), 일회성이면 날짜(10/12 (월))
    private List<SchedulePollColumnResponseDto> columns(SchedulePoll poll) {
        return SchedulePollSlotUtils.columnKeys(poll).stream()
                .map(key -> new SchedulePollColumnResponseDto(key, columnLabel(poll.isRecurring(), key)))
                .toList();
    }

    private String columnLabel(boolean recurring, String key) {
        if (recurring) {
            return PollDay.valueOf(key).getLabel();
        }
        LocalDate date = LocalDate.parse(key);
        String day = PollDay.values()[date.getDayOfWeek().getValue() - 1].getLabel();
        return date.getMonthValue() + "/" + date.getDayOfMonth() + " (" + day + ")";
    }

    // 표시 이름: 실명이 있으면 실명, 없으면 닉네임
    // TODO(8단계): 실명 필드가 생기면 실명을 먼저 쓴다.
    private String displayName(Member member) {
        return member.getNickname();
    }

    // 형식(HH:mm)은 DTO에서 검증했다. 30분 단위가 아니거나 24:00을 넘으면 INVALID_RANGE
    private int toRangeMinutes(String time) {
        try {
            return SchedulePollSlotUtils.toMinutes(time);
        } catch (IllegalArgumentException e) {
            throw new SchedulePollException(SchedulePollErrorCode.INVALID_RANGE);
        }
    }

    // FIXED는 30·60·90 중 하나, AT_LEAST는 30분 단위(30 이상). 정해진 값이 아니면 VALIDATION_ERROR
    // 시간대보다 긴지는 시간대 검사에서 INVALID_RANGE로 본다.
    private void validateDuration(DurationMode mode, int durationMin) {
        boolean valid = mode == DurationMode.FIXED
                ? FIXED_DURATIONS.contains(durationMin)
                : durationMin >= SchedulePollSlotUtils.SLOT_MINUTES && durationMin % SchedulePollSlotUtils.SLOT_MINUTES == 0;
        if (!valid) {
            throw new SchedulePollException(SchedulePollErrorCode.VALIDATION_ERROR);
        }
    }

    // 요일 1~7개, 중복 불가
    private void validateDays(List<PollDay> days) {
        if (days == null || days.isEmpty() || days.stream().anyMatch(Objects::isNull)
                || new HashSet<>(days).size() != days.size()) {
            throw new SchedulePollException(SchedulePollErrorCode.INVALID_RANGE);
        }
    }

    // 오늘 ≤ 시작일 ≤ 종료일, 기간 최대 14일
    private void validateDates(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate == null || endDate == null || startDate.isBefore(today) || endDate.isBefore(startDate)
                || ChronoUnit.DAYS.between(startDate, endDate) + 1 > MAX_ONE_TIME_DAYS) {
            throw new SchedulePollException(SchedulePollErrorCode.INVALID_RANGE);
        }
    }

    // 22자 URL-safe 랜덤 토큰. 겹치면 다시 만든다.
    private String generateLinkToken() {
        while (true) {
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            if (!schedulePollRepository.existsByLinkToken(token)) {
                return token;
            }
        }
    }

    private String linkUrl(String linkToken) {
        return baseUrl + "/schedule-polls/join/" + linkToken;
    }

    private static LocalDateTime toSeoul(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(SEOUL).toLocalDateTime();
    }

    private static OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(SEOUL).toOffsetDateTime();
    }
}
