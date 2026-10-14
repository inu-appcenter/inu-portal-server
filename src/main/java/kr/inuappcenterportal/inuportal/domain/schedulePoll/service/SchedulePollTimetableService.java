package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollAddToTimetableResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollResultResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.PollDay;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.ResultState;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollErrorCode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.exception.SchedulePollException;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollResult;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePollTimetableAdd;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollResultRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollSubmissionRepository;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.repository.SchedulePollTimetableAddRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem.TimeTableCustomItemCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.timeTableItem.TimeTableCustomMeetingRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTableItem;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.service.TimeTableItemService;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

// 11. 결과를 내 대표 시간표에 매주 반복 일정으로 추가한다.
@Service
@RequiredArgsConstructor
public class SchedulePollTimetableService {

    private static final Set<MyErrorCode> TIME_CONFLICT_CODES = Set.of(
            MyErrorCode.TIMETABLE_ITEM_TIME_CONFLICT,
            MyErrorCode.TIMETABLE_ITEM_TIME_REQUEST_CONFLICT,
            MyErrorCode.TIMETABLE_ITEM_TIME_DB_CONFLICT);
    // 시간표는 LocalTime이라 24:00을 담을 수 없어 23:59로 저장한다.
    private static final LocalTime DAY_END = LocalTime.of(23, 59);

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollSubmissionRepository schedulePollSubmissionRepository;
    private final SchedulePollResultRepository schedulePollResultRepository;
    private final SchedulePollTimetableAddRepository schedulePollTimetableAddRepository;
    private final SemesterRepository semesterRepository;
    private final TimeTableRepository timeTableRepository;
    private final TimeTableItemRepository timeTableItemRepository;
    private final TimeTableItemService timeTableItemService;
    private final ObjectMapper objectMapper;

    // 참여자 → 최신·최종 버전 → 이미 추가함 → 일회성 → 대표 시간표 순으로 판정한다(명세서 순서).
    @Transactional
    public SchedulePollAddToTimetableResponseDto add(Member member, Long pollId, int version, int candidateRank) {
        // 같은 조율의 추가 요청을 한 번에 하나씩 처리해 연타해도 일정이 하나만 생기게 한다.
        SchedulePoll poll = schedulePollRepository.findByIdForUpdate(pollId)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));
        if (!schedulePollSubmissionRepository.existsByPollIdAndMemberId(pollId, member.getId())) {
            throw new SchedulePollException(SchedulePollErrorCode.NOT_PARTICIPANT);
        }
        SchedulePollResult result = schedulePollResultRepository.findByPollIdAndVersion(pollId, version)
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.POLL_NOT_FOUND));
        if (result.getState() != ResultState.LATEST && !result.isFinal()) {
            throw new SchedulePollException(SchedulePollErrorCode.RESULT_STALE);
        }

        // 이 결과(버전)에서 이미 추가했고 그 일정이 시간표에 남아 있으면 그대로 돌려준다. 지워졌으면 다시 추가한다.
        // 한 결과에서 정해진 회의 시간은 하나라서, 다른 순위를 요청해도 이미 추가한 일정을 돌려준다(명세서 R12).
        Optional<SchedulePollTimetableAdd> history =
                schedulePollTimetableAddRepository.findByMemberIdAndPollIdAndVersion(member.getId(), pollId, version);
        if (history.isPresent() && timeTableItemRepository.existsById(history.get().getTimetableItemId())) {
            return new SchedulePollAddToTimetableResponseDto(history.get().getCustomScheduleId(), true);
        }

        if (!poll.isRecurring()) {
            throw new SchedulePollException(SchedulePollErrorCode.ONE_TIME_NOT_SUPPORTED);
        }
        TimeTable timeTable = currentSemester()
                .flatMap(semester -> timeTableRepository.findByMemberIdAndSemesterIdAndIsPrimaryTrue(member.getId(), semester.getId()))
                .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.NO_PRIMARY_TIMETABLE));

        SchedulePollResultResponseDto.Candidate candidate = findCandidate(result, candidateRank);
        TimeTableItem item = createCustomItem(member, timeTable, poll.getTitle(), candidate);
        Long customScheduleId = item.getCustomSchedule().getId();

        if (history.isPresent()) {
            history.get().replace(candidateRank, item.getId(), customScheduleId);
        } else {
            schedulePollTimetableAddRepository.save(SchedulePollTimetableAdd.create(
                    member.getId(), pollId, version, candidateRank, item.getId(), customScheduleId));
        }
        return new SchedulePollAddToTimetableResponseDto(customScheduleId, false);
    }

    // 기존 시간표 일정 추가 기능을 그대로 쓴다(내 시간표 확인, 겹침 검사 포함). 겹치면 409 TIMETABLE_CONFLICT
    private TimeTableItem createCustomItem(Member member, TimeTable timeTable, String title,
                                           SchedulePollResultResponseDto.Candidate candidate) {
        DayOfWeek day = DayOfWeek.values()[PollDay.valueOf(candidate.key()).ordinal()];
        LocalTime start = LocalTime.parse(candidate.start());
        LocalTime end = candidate.end().equals("24:00") ? DAY_END : LocalTime.parse(candidate.end());
        TimeTableCustomItemCreateRequestDto request = new TimeTableCustomItemCreateRequestDto(
                title, null, List.of(new TimeTableCustomMeetingRequestDto(null, day, start, end)));
        try {
            Long itemId = timeTableItemService.createTimeTableItemForCustom(member.getId(), timeTable.getId(), request).id();
            return timeTableItemRepository.findById(itemId).orElseThrow();
        } catch (MyException e) {
            if (TIME_CONFLICT_CODES.contains(e.getErrorCode())) {
                throw new SchedulePollException(SchedulePollErrorCode.TIMETABLE_CONFLICT);
            }
            throw e;
        }
    }

    // 진행 중(OPEN) 학기, 없으면 가장 최근 학기 (기존 시간표 인식 기능과 같은 규칙)
    private Optional<Semester> currentSemester() {
        return semesterRepository.findFirstByStatusOrderByStartDateDesc(SemesterStatus.OPEN)
                .or(() -> semesterRepository.findAllByOrderByStartDateDesc().stream().findFirst());
    }

    private SchedulePollResultResponseDto.Candidate findCandidate(SchedulePollResult result, int rank) {
        try {
            List<SchedulePollResultResponseDto.Candidate> candidates =
                    objectMapper.readValue(result.getCandidates(), new TypeReference<>() {
                    });
            return candidates.stream()
                    .filter(c -> c.rank() == rank)
                    .findFirst()
                    .orElseThrow(() -> new SchedulePollException(SchedulePollErrorCode.VALIDATION_ERROR));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("결과 JSON 읽기 실패", e);
        }
    }
}
