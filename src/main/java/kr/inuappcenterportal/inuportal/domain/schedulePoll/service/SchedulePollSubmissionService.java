package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.AvailableRangeDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request.SchedulePollSubmitRequestDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollMySubmissionResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollParticipantsResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollSubmitResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ParticipantStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SubmissionVia;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollErrorCode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollException;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollSubmission;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollSubmitRequest;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollSubmissionRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollSubmitRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollDisplayUtils.displayName;
import static kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollDisplayUtils.maskStudentId;
import static kr.inuappcenterportal.inuportal.domain.schedulePoll.service.SchedulePollDisplayUtils.toOffset;

@Service
@RequiredArgsConstructor
public class SchedulePollSubmissionService {

    private static final int MIN_PARTICIPANTS_FOR_RESULT = 2;

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollSubmissionRepository schedulePollSubmissionRepository;
    private final SchedulePollSubmitRequestRepository schedulePollSubmitRequestRepository;
    private final SchedulePollTransactionService schedulePollTransactionService;
    private final SchedulePollResultService schedulePollResultService;
    private final SchedulePollService schedulePollService;
    private final ObjectMapper objectMapper;

    // 5. 내 제출본. 제출한 적이 없으면 404 SUBMISSION_NOT_FOUND(앱은 빈 그리드로 시작)
    @Transactional(readOnly = true)
    public SchedulePollMySubmissionResponseDto getMine(Member member, Long pollId) {
        if (!schedulePollRepository.existsById(pollId)) {
            throw new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND);
        }
        SchedulePollSubmission submission = schedulePollSubmissionRepository.findByPollIdAndMemberId(pollId, member.getId())
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.SUBMISSION_NOT_FOUND));
        return new SchedulePollMySubmissionResponseDto(
                ParticipantStatus.SUBMITTED,
                submission.getSubmissionRevision(),
                readJson(submission.getAvailable(), new TypeReference<>() {
                }),
                submission.getImportedCourseOfferingIds() == null ? List.of()
                        : readJson(submission.getImportedCourseOfferingIds(), new TypeReference<>() {
                }),
                toOffset(submission.getSubmittedAt()));
    }

    // 6. 제출·재제출(= 참여). 공통 처리(잠금 → 끝남·마감 확인 → revision + 1) 위에서 돈다.
    // 트랜잭션은 SchedulePollTransactionService가 연다. 이 메서드에 @Transactional을 붙이지 않는다.
    public SchedulePollSubmitResponseDto submit(Member member, Long pollId, SchedulePollSubmitRequestDto request) {
        String clientSubmissionId = request.clientSubmissionId().toString();
        return schedulePollTransactionService.execute(pollId, new SchedulePollTransactionService.Write<>() {

            // 이미 처리한 요청이면 처음 응답을 그대로 돌려준다(끝남·마감 확인보다 먼저).
            @Override
            public Optional<SchedulePollSubmitResponseDto> beforeStatusCheck(SchedulePoll poll) {
                return schedulePollSubmitRequestRepository.findByClientSubmissionId(clientSubmissionId)
                        .map(saved -> {
                            if (!saved.getPollId().equals(pollId) || !saved.getMemberId().equals(member.getId())) {
                                throw new SchedulePollException(SchedulePollErrorCode.VALIDATION_ERROR);
                            }
                            return readJson(saved.getResponseJson(), new TypeReference<>() {
                            });
                        });
            }

            @Override
            public SchedulePollSubmitResponseDto apply(SchedulePoll poll, LocalDateTime now) {
                return submitLocked(member, poll, request, clientSubmissionId, now);
            }
        });
    }

    private SchedulePollSubmitResponseDto submitLocked(Member member, SchedulePoll poll, SchedulePollSubmitRequestDto request,
                                                       String clientSubmissionId, LocalDateTime now) {
        // 개설자와 차단 관계
        if (schedulePollService.isBlockedEitherWay(member, poll.getCreator())) {
            throw new SchedulePollException(SchedulePollErrorCode.BLOCKED);
        }
        SchedulePollSubmission submission = schedulePollSubmissionRepository
                .findByPollIdAndMemberId(poll.getId(), member.getId())
                .orElse(null);
        boolean isFirst = submission == null;
        if (isFirst) {
            // 첫 제출인데 이미 10명
            if (poll.getParticipantCount() >= SchedulePoll.MAX_PARTICIPANTS) {
                throw new SchedulePollException(SchedulePollErrorCode.POLL_FULL);
            }
            if (request.joinSource() == null) {
                throw new SchedulePollException(SchedulePollErrorCode.VALIDATION_ERROR);
            }
        } else if (request.baseSubmissionRevision() != null
                && request.baseSubmissionRevision() != submission.getSubmissionRevision()) {
            // 다른 기기에서 먼저 고쳤음. base가 null이면(처음 제출이 두 번 옴) 덮어쓴다.
            throw new SchedulePollException(SchedulePollErrorCode.SUBMISSION_CONFLICT);
        }

        List<AvailableRangeDto> available = request.via() == SubmissionVia.ANYTIME
                ? fullRange(poll)
                : SchedulePollSlotUtils.normalize(poll, request.available());
        String availableJson = writeJson(available);
        String importedJson = request.importedCourseOfferingIds() == null ? null : writeJson(request.importedCourseOfferingIds());

        if (isFirst) {
            submission = schedulePollSubmissionRepository.save(SchedulePollSubmission.create(
                    poll, member, availableJson, importedJson, request.via(), request.joinSource(), now));
            poll.addParticipant();
        } else {
            submission.resubmit(availableJson, importedJson, request.via(), now);
        }
        schedulePollSubmissionRepository.flush();

        Integer resultVersion = null;
        if (poll.getParticipantCount() >= MIN_PARTICIPANTS_FOR_RESULT) {
            resultVersion = schedulePollResultService.createNewVersion(poll);
        }

        SchedulePollSubmitResponseDto response = new SchedulePollSubmitResponseDto(
                ParticipantStatus.SUBMITTED,
                isFirst,
                submission.getSubmissionRevision(),
                poll.getStatus(),
                poll.getParticipantCount(),
                resultVersion,
                poll.getRevision());
        schedulePollSubmitRequestRepository.save(SchedulePollSubmitRequest.create(
                clientSubmissionId, poll.getId(), member.getId(), writeJson(response)));
        return response;
    }

    // 7. 참여자 목록. 로그인한 사람 누구나 볼 수 있다(이름과 인원만). 제출하지 않은 개설자는 없다.
    @Transactional(readOnly = true)
    public SchedulePollParticipantsResponseDto getParticipants(Long pollId) {
        SchedulePoll poll = schedulePollRepository.findById(pollId)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));
        Long creatorId = poll.getCreator().getId();
        List<SchedulePollParticipantsResponseDto.Participant> participants =
                schedulePollSubmissionRepository.findAllInJoinOrder(pollId).stream()
                        .map(s -> new SchedulePollParticipantsResponseDto.Participant(
                                s.getMember().getId(),
                                displayName(s.getMember()),
                                maskStudentId(s.getMember().getStudentId()),
                                s.getMember().getId().equals(creatorId),
                                toOffset(s.getJoinedAt()),
                                toOffset(s.getSubmittedAt())))
                        .toList();
        return new SchedulePollParticipantsResponseDto(poll.getParticipantCount(), SchedulePoll.MAX_PARTICIPANTS, participants);
    }

    // "언제든 괜찮아요": 모든 열 × 전체 시간대
    private List<AvailableRangeDto> fullRange(SchedulePoll poll) {
        String start = SchedulePollSlotUtils.formatMinutes(poll.getRangeStartMinutes());
        String end = SchedulePollSlotUtils.formatMinutes(poll.getRangeEndMinutes());
        return SchedulePollSlotUtils.columnKeys(poll).stream()
                .map(key -> new AvailableRangeDto(key, start, end))
                .toList();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("제출 JSON 변환 실패", e);
        }
    }

    private <T> T readJson(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("제출 JSON 읽기 실패", e);
        }
    }
}
