package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.AvailableRangeDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollResultResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ResultState;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollErrorCode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollException;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollResult;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollSubmission;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollResultRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollDisplayUtils.columns;
import static kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollDisplayUtils.displayName;
import static kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollDisplayUtils.toOffset;

@Service
@RequiredArgsConstructor
public class SchedulePollResultService {

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollSubmissionRepository schedulePollSubmissionRepository;
    private final SchedulePollResultRepository schedulePollResultRepository;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper;

    // 결과를 새로 계산해 새 버전(LATEST)으로 저장하고 이전 버전은 STALE로 바꾼다. 결과 번호를 돌려준다.
    // 조율 행을 잠근 트랜잭션 안에서만 호출한다. 계산 중 예외가 나면 전체가 롤백되고 500이 된다.
    public int createNewVersion(SchedulePoll poll) {
        List<SchedulePollSubmission> submissions = schedulePollSubmissionRepository.findAllInJoinOrder(poll.getId());
        List<List<AvailableRangeDto>> ranges = new ArrayList<>();
        List<Long> memberIds = new ArrayList<>();
        for (SchedulePollSubmission submission : submissions) {
            ranges.add(readJson(submission.getAvailable(), new TypeReference<>() {
            }));
            memberIds.add(submission.getMember().getId());
        }
        LocalDateTime basedOnSubmittedAt = submissions.stream()
                .map(SchedulePollSubmission::getSubmittedAt)
                .max(Comparator.naturalOrder())
                .orElseThrow();

        SchedulePollResultUtils.Result result = SchedulePollResultUtils.calculate(poll, ranges);

        schedulePollResultRepository.findByPollIdAndState(poll.getId(), ResultState.LATEST)
                .ifPresent(SchedulePollResult::markStale);
        int version = poll.nextResultVersion();
        schedulePollResultRepository.save(SchedulePollResult.create(poll, version, basedOnSubmittedAt,
                writeJson(memberIds), writeJson(result.heatmap()), writeJson(result.candidates())));
        return version;
    }

    // 조율이 끝날 때 최신 결과를 최종 결과로 고정한다. 결과가 없으면(참여자 2명 미만) 아무것도 하지 않는다.
    // 조율 행을 잠근 트랜잭션 안에서만 호출한다.
    public void finalizeLatest(SchedulePoll poll) {
        schedulePollResultRepository.findByPollIdAndState(poll.getId(), ResultState.LATEST)
                .ifPresent(SchedulePollResult::markFinal);
    }

    // 8. 최신 결과. 참여자만 볼 수 있고, 참여자 2명 미만이라 결과가 없으면 409 RESULT_NOT_READY
    @Transactional(readOnly = true)
    public SchedulePollResultResponseDto getLatest(Member member, Long pollId) {
        SchedulePoll poll = findPollForParticipant(member, pollId);
        if (poll.getResultVersion() == 0) {
            throw new SchedulePollException(SchedulePollErrorCode.RESULT_NOT_READY);
        }
        return toResponse(poll, findResult(pollId, poll.getResultVersion()));
    }

    // 8. 특정 버전 결과. 없는 버전이면 404 POLL_NOT_FOUND
    @Transactional(readOnly = true)
    public SchedulePollResultResponseDto getVersion(Member member, Long pollId, int version) {
        SchedulePoll poll = findPollForParticipant(member, pollId);
        return toResponse(poll, findResult(pollId, version));
    }

    private SchedulePoll findPollForParticipant(Member member, Long pollId) {
        SchedulePoll poll = schedulePollRepository.findById(pollId)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));
        if (!schedulePollSubmissionRepository.existsByPollIdAndMemberId(pollId, member.getId())) {
            throw new SchedulePollException(SchedulePollErrorCode.NOT_PARTICIPANT);
        }
        return poll;
    }

    private SchedulePollResult findResult(Long pollId, int version) {
        return schedulePollResultRepository.findByPollIdAndVersion(pollId, version)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));
    }

    // 참여자 이름은 결과를 만든 때가 아니라 지금 이름으로 보여 준다.
    private SchedulePollResultResponseDto toResponse(SchedulePoll poll, SchedulePollResult result) {
        List<Long> memberIds = readJson(result.getParticipantMemberIds(), new TypeReference<>() {
        });
        Map<Long, Member> members = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        List<SchedulePollResultResponseDto.Participant> participants = new ArrayList<>();
        for (int index = 0; index < memberIds.size(); index++) {
            Long memberId = memberIds.get(index);
            Member member = members.get(memberId);
            participants.add(new SchedulePollResultResponseDto.Participant(
                    index, memberId, member == null ? null : displayName(member)));
        }

        return new SchedulePollResultResponseDto(
                result.getVersion(),
                result.getState(),
                result.isFinal(),
                toOffset(result.getBasedOnSubmittedAt()),
                poll.getDurationMode(),
                poll.getDurationMinutes(),
                participants,
                columns(poll),
                SchedulePollSlotUtils.formatMinutes(poll.getRangeStartMinutes()),
                (poll.getRangeEndMinutes() - poll.getRangeStartMinutes()) / SchedulePollSlotUtils.SLOT_MINUTES,
                readJson(result.getHeatmap(), new TypeReference<>() {
                }),
                readJson(result.getCandidates(), new TypeReference<>() {
                }));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("결과 JSON 변환 실패", e);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("결과 JSON 읽기 실패", e);
        }
    }
}
