package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hypersistence.tsid.TSID;
import kr.inuappcenterportal.inuportal.domain.chat.dto.PersonalChatRoomRequestDto;
import kr.inuappcenterportal.inuportal.domain.chat.enums.ChatMemberStatus;
import kr.inuappcenterportal.inuportal.domain.chat.enums.MessageType;
import kr.inuappcenterportal.inuportal.domain.chat.repository.ChatRoomMemberRepository;
import kr.inuappcenterportal.inuportal.domain.chat.service.ChatRoomService;
import kr.inuappcenterportal.inuportal.domain.member.enums.FriendStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Friend;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.BlockRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.FriendRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollShareRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollResultResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollShareResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.PollDay;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ShareKind;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollErrorCode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollException;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollResult;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollShare;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollResultRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollShareRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollSubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// 9. 카드 공유. 채팅방이나 친구(1:1 방)에 참여 요청·결과 카드를 보낸다.
// 공유 기록 저장까지 한 트랜잭션에서 하고, 커밋된 뒤에 채팅 메시지(실시간 전송·푸시)를 보낸다.
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulePollShareService {

    private static final int MAX_TARGETS = 10;
    private static final int MAX_CANDIDATE_RANK = 3;
    private static final String INVITE_CONTENT = "회의 시간 조율하게 가능한 시간 체크해주세요!";

    private final SchedulePollTransactionService schedulePollTransactionService;
    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollSubmissionRepository schedulePollSubmissionRepository;
    private final SchedulePollResultRepository schedulePollResultRepository;
    private final SchedulePollShareRepository schedulePollShareRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomService chatRoomService;
    private final FriendRepository friendRepository;
    private final BlockRepository blockRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    // 트랜잭션은 SchedulePollTransactionService가 연다. 이 메서드에 @Transactional을 붙이지 않는다.
    public SchedulePollShareResponseDto share(Member member, Long pollId, SchedulePollShareRequestDto request) {
        validateShape(request);
        SharePlan plan = schedulePollTransactionService.execute(pollId, new SchedulePollTransactionService.Write<>() {

            // 끝난 조율: 참여 요청 카드는 409 POLL_CLOSED, 결과 카드는 최종 버전일 때만 보낸다.
            @Override
            public Optional<SharePlan> beforeStatusCheck(SchedulePoll poll) {
                if (!poll.isClosed()) {
                    return Optional.empty();
                }
                if (request.kind() == ShareKind.INVITE) {
                    throw new SchedulePollException(SchedulePollErrorCode.POLL_CLOSED);
                }
                requireParticipant(member, poll);
                if (poll.getResultVersion() == 0) {
                    throw new SchedulePollException(SchedulePollErrorCode.RESULT_NOT_READY);
                }
                SchedulePollResult result = schedulePollResultRepository
                        .findByPollIdAndVersion(poll.getId(), request.resultVersion())
                        .filter(SchedulePollResult::isFinal)
                        .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.RESULT_STALE));
                return Optional.of(prepare(member, poll, request, result));
            }

            @Override
            public SharePlan apply(SchedulePoll poll, LocalDateTime now) {
                if (request.kind() == ShareKind.INVITE) {
                    // 참여 요청 카드: 개설자 또는 참여자
                    boolean isHost = poll.getCreator().getId().equals(member.getId());
                    if (!isHost && !isParticipant(member, poll)) {
                        throw new SchedulePollException(SchedulePollErrorCode.NOT_HOST);
                    }
                    return prepare(member, poll, request, null);
                }
                // 결과 카드: 참여자만, 최신 버전만
                requireParticipant(member, poll);
                if (poll.getResultVersion() == 0) {
                    throw new SchedulePollException(SchedulePollErrorCode.RESULT_NOT_READY);
                }
                if (request.resultVersion() != poll.getResultVersion()) {
                    throw new SchedulePollException(SchedulePollErrorCode.RESULT_STALE);
                }
                SchedulePollResult result = schedulePollResultRepository
                        .findByPollIdAndVersion(poll.getId(), request.resultVersion())
                        .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.RESULT_STALE));
                return prepare(member, poll, request, result);
            }
        });

        // 커밋된 뒤 새 카드만 실제로 보낸다. 한 방이 실패해도 나머지는 보낸다.
        // 보내지 못한 방은 공유 기록을 지워, 같은 요청을 다시 보냈을 때 "이미 보낸 카드"로 처리되지 않게 한다.
        Set<Long> failedRoomIds = new HashSet<>();
        for (Long[] pending : plan.pending()) {
            try {
                chatRoomService.sendServerMessage(pending[1], pending[0], member.getId(),
                        plan.content(), plan.messageType(), plan.extraData());
            } catch (Exception e) {
                log.error("[SchedulePollShare] 카드 메시지 발송 실패 pollId={}, roomId={}", pollId, pending[0], e);
                failedRoomIds.add(pending[0]);
                discardShare(pollId, pending[0], pending[1]);
            }
        }
        if (failedRoomIds.isEmpty()) {
            return plan.response();
        }
        // 응답에는 실제로 카드가 있는 방만 남긴다. 한 곳도 못 보냈으면 실패로 응답해 다시 시도할 수 있게 한다.
        List<SchedulePollShareResponseDto.Result> delivered = plan.response().results().stream()
                .filter(result -> !failedRoomIds.contains(result.chatRoomId()))
                .toList();
        if (delivered.isEmpty()) {
            throw new SchedulePollException(SchedulePollErrorCode.INTERNAL_ERROR);
        }
        return new SchedulePollShareResponseDto(delivered);
    }

    private void discardShare(Long pollId, Long roomId, Long messageId) {
        try {
            schedulePollShareRepository.deleteByPollIdAndChatMessageId(pollId, messageId);
        } catch (Exception e) {
            log.error("[SchedulePollShare] 발송 실패한 공유 기록 삭제 실패 pollId={}, roomId={}, messageId={}",
                    pollId, roomId, messageId, e);
        }
    }

    // 마감 1시간 전 알림 준비(스케줄러용). 조율을 잠그고 다시 확인한 뒤 알림 시각을 기록하고,
    // 카드가 공유된 방마다 보낼 메시지를 돌려준다. 실제 발송은 커밋 뒤 sendReminder로 한다.
    @Transactional
    public List<Reminder> prepareReminder(Long pollId) {
        SchedulePoll poll = schedulePollRepository.findByIdForUpdate(pollId).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        if (poll == null || !poll.isReminderDue(now)) {
            return List.of();
        }
        poll.markReminded(now);

        String content = "⏰ 「" + poll.getTitle() + "」 시간 맞추기 마감 1시간 전이에요";
        // 방마다 카드를 먼저 공유한 사람 중 지금도 방에 있는 사람 이름으로 보낸다. 아무도 없으면 그 방은 건너뛴다.
        Map<Long, Long> senderByRoom = new LinkedHashMap<>();
        for (SchedulePollShare share : schedulePollShareRepository.findAllByPollIdOrderByIdAsc(pollId)) {
            if (!senderByRoom.containsKey(share.getChatRoomId()) && chatRoomMemberRepository
                    .existsByChatRoomIdAndMemberIdAndStatus(share.getChatRoomId(), share.getSharedBy(), ChatMemberStatus.JOINED)) {
                senderByRoom.put(share.getChatRoomId(), share.getSharedBy());
            }
        }
        return senderByRoom.entrySet().stream()
                .map(e -> new Reminder(e.getKey(), e.getValue(), content))
                .toList();
    }

    // prepareReminder가 커밋된 뒤 호출한다. 한 방이 실패해도 나머지는 보낸다.
    public void sendReminder(List<Reminder> reminders) {
        for (Reminder reminder : reminders) {
            try {
                chatRoomService.sendServerMessage(TSID.fast().toLong(), reminder.roomId(), reminder.senderId(),
                        reminder.content(), MessageType.TEXT, null);
            } catch (Exception e) {
                log.error("[SchedulePollShare] 마감 1시간 전 알림 발송 실패 roomId={}", reminder.roomId(), e);
            }
        }
    }

    public record Reminder(Long roomId, Long senderId, String content) {
    }

    // 대상 확정 → 권한 확인 → 방마다 카드 기록(이미 있으면 재사용). 하나라도 실패하면 예외로 전체 취소된다.
    private SharePlan prepare(Member member, SchedulePoll poll, SchedulePollShareRequestDto request, SchedulePollResult result) {
        String content;
        MessageType messageType;
        String extraData;
        int version;
        if (request.kind() == ShareKind.INVITE) {
            content = INVITE_CONTENT;
            messageType = MessageType.SCHEDULE_POLL_INVITE;
            extraData = writeJson(Map.of("pollId", poll.getId()));
            version = 0;
        } else {
            SchedulePollResultResponseDto.Candidate candidate = findCandidate(result, request.candidateRank());
            content = "회의 시간이 정해졌어요: " + candidateLabel(poll, candidate);
            messageType = MessageType.SCHEDULE_POLL_RESULT;
            Map<String, Object> extra = new LinkedHashMap<>();
            extra.put("pollId", poll.getId());
            extra.put("resultVersion", result.getVersion());
            extra.put("candidateRank", request.candidateRank());
            extraData = writeJson(extra);
            version = result.getVersion();
        }

        // 방 ID → 친구 회원 ID(친구로 보낸 경우). 같은 방은 한 번만
        Map<Long, Long> targets = new LinkedHashMap<>();
        List<Long> chatRoomIds = request.chatRoomIds() == null ? List.of() : request.chatRoomIds();
        for (Long roomId : new LinkedHashSet<>(chatRoomIds)) {
            targets.put(roomId, null);
        }
        List<Long> friendIds = request.friendMemberIds() == null ? List.of() : request.friendMemberIds();
        for (Long friendMemberId : new LinkedHashSet<>(friendIds)) {
            targets.putIfAbsent(personalRoomId(member, friendMemberId), friendMemberId);
        }
        if (targets.size() > MAX_TARGETS) {
            throw new SchedulePollException(SchedulePollErrorCode.TOO_MANY_TARGETS);
        }
        for (Long roomId : targets.keySet()) {
            if (!chatRoomMemberRepository.existsByChatRoomIdAndMemberIdAndStatus(roomId, member.getId(), ChatMemberStatus.JOINED)) {
                throw new SchedulePollException(SchedulePollErrorCode.NOT_SHARE_TARGET);
            }
        }

        List<SchedulePollShareResponseDto.Result> results = new ArrayList<>();
        List<Long[]> pending = new ArrayList<>();
        for (Map.Entry<Long, Long> target : targets.entrySet()) {
            Long roomId = target.getKey();
            Optional<SchedulePollShare> existing = schedulePollShareRepository
                    .findByPollIdAndKindAndResultVersionAndChatRoomId(poll.getId(), request.kind(), version, roomId);
            if (existing.isPresent()) {
                results.add(new SchedulePollShareResponseDto.Result(roomId, existing.get().getChatMessageId(), false, target.getValue()));
                continue;
            }
            Long messageId = TSID.fast().toLong();
            schedulePollShareRepository.save(SchedulePollShare.create(
                    poll.getId(), request.kind(), version, roomId, messageId, member.getId()));
            pending.add(new Long[]{roomId, messageId});
            results.add(new SchedulePollShareResponseDto.Result(roomId, messageId, true, target.getValue()));
        }
        return new SharePlan(new SchedulePollShareResponseDto(results), pending, content, messageType, extraData);
    }

    // 친구 → 1:1 방(없으면 생성). 친구가 아니거나 차단 관계면 403 NOT_SHARE_TARGET
    private Long personalRoomId(Member member, Long friendMemberId) {
        if (friendMemberId == null || friendMemberId.equals(member.getId())) {
            throw new SchedulePollException(SchedulePollErrorCode.NOT_SHARE_TARGET);
        }
        Member friend = memberRepository.findById(friendMemberId)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.NOT_SHARE_TARGET));
        Friend relation = friendRepository.findByRequesterAndReceiver(member, friend)
                .or(() -> friendRepository.findByRequesterAndReceiver(friend, member))
                .filter(f -> f.getStatus() == FriendStatus.ACCEPTED)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.NOT_SHARE_TARGET));
        if (blockRepository.existsByBlockerAndBlocked(member, friend) || blockRepository.existsByBlockerAndBlocked(friend, member)) {
            throw new SchedulePollException(SchedulePollErrorCode.NOT_SHARE_TARGET);
        }
        // 기존 1:1 방 찾기·만들기를 그대로 쓴다. 요청 DTO에 생성자가 없어 HTTP 요청과 같은 방식(JSON)으로 만든다.
        PersonalChatRoomRequestDto roomRequest = objectMapper.convertValue(
                Map.of("targetFriendIds", List.of(relation.getId())), PersonalChatRoomRequestDto.class);
        return chatRoomService.getOrCreatePersonalChatRoom(roomRequest, member.getId()).getId();
    }

    private SchedulePollResultResponseDto.Candidate findCandidate(SchedulePollResult result, int rank) {
        List<SchedulePollResultResponseDto.Candidate> candidates = readJson(result.getCandidates(), new TypeReference<>() {
        });
        return candidates.stream()
                .filter(c -> c.rank() == rank)
                .findFirst()
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.VALIDATION_ERROR));
    }

    // 정기: "월 10:30~11:30" / 일회성: "10/12(월) 10:30~11:30"
    private String candidateLabel(SchedulePoll poll, SchedulePollResultResponseDto.Candidate candidate) {
        String column;
        if (poll.isRecurring()) {
            column = PollDay.valueOf(candidate.key()).getLabel();
        } else {
            LocalDate date = LocalDate.parse(candidate.key());
            String day = PollDay.values()[date.getDayOfWeek().getValue() - 1].getLabel();
            column = date.getMonthValue() + "/" + date.getDayOfMonth() + "(" + day + ")";
        }
        return column + " " + candidate.start() + "~" + candidate.end();
    }

    // 결과 카드는 resultVersion·candidateRank(1~3) 필수, 대상은 하나 이상
    private void validateShape(SchedulePollShareRequestDto request) {
        boolean noTargets = (request.chatRoomIds() == null || request.chatRoomIds().isEmpty())
                && (request.friendMemberIds() == null || request.friendMemberIds().isEmpty());
        boolean badResult = request.kind() == ShareKind.RESULT && (request.resultVersion() == null
                || request.candidateRank() == null
                || request.candidateRank() < 1 || request.candidateRank() > MAX_CANDIDATE_RANK);
        if (noTargets || badResult) {
            throw new SchedulePollException(SchedulePollErrorCode.VALIDATION_ERROR);
        }
    }

    private boolean isParticipant(Member member, SchedulePoll poll) {
        return schedulePollSubmissionRepository.existsByPollIdAndMemberId(poll.getId(), member.getId());
    }

    private void requireParticipant(Member member, SchedulePoll poll) {
        if (!isParticipant(member, poll)) {
            throw new SchedulePollException(SchedulePollErrorCode.NOT_PARTICIPANT);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("카드 JSON 변환 실패", e);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("카드 JSON 읽기 실패", e);
        }
    }

    // pending: {방 ID, 미리 정한 메시지 ID}. 커밋 뒤 이 방들에만 실제 메시지를 보낸다.
    private record SharePlan(SchedulePollShareResponseDto response, List<Long[]> pending,
                             String content, MessageType messageType, String extraData) {
    }
}
