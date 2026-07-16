package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableCreateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableNameUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.request.TimeTableVisibilityUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableItemRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TimeTableService {

    private final TimeTableRepository timeTableRepository;
    private final TimeTableItemRepository timeTableItemRepository;
    private final MemberRepository memberRepository;
    private final SemesterRepository semesterRepository;


    /**
     * 시간표 생성 메서드
     */
    @Transactional
    public TimeTableResponseDto createTimeTable(
            Long memberId,
            Long semesterId,
            TimeTableCreateRequestDto createRequestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학기입니다."));

        checkDuplicateTimeTableNameForCreate(memberId, semesterId, createRequestDto.timeTableName());

        boolean isFirstTimeTable = !timeTableRepository.existsByMemberIdAndSemesterId(memberId, semesterId);

        TimeTable timeTable = TimeTable.create(
                createRequestDto.timeTableName(),
                isFirstTimeTable,
                member,
                semester
        );

        TimeTable saved = timeTableRepository.save(timeTable);

        return TimeTableResponseDto.from(saved);
    }

    /**
     * 시간표 이름 중복 확인 메서드(생성용)
     */
    private void checkDuplicateTimeTableNameForCreate(
            Long memberId,
            Long semesterId,
            String checkName
    ) {
        if (timeTableRepository.existsByMemberIdAndSemesterIdAndTimeTableName(
                memberId,
                semesterId,
                checkName
        )) {
            throw new IllegalArgumentException("이미 같은 이름의 시간표가 존재합니다.");
        }
    }

    /**
     * 시간표 이름 중복 확인 메서드(수정용)
     */
    private void checkDuplicateTimeTableNameForUpdate(
            Long memberId,
            Long semesterId,
            String checkName,
            Long timeTableId
    ) {
        if (timeTableRepository.existsByMemberIdAndSemesterIdAndTimeTableNameAndIdNot(
                memberId,
                semesterId,
                checkName,
                timeTableId
        )) {
            throw new IllegalArgumentException("이미 같은 이름의 시간표가 존재합니다.");
        }
    }


    /**
     * 시간표 공개범위 수정 메서드
     */
    @Transactional
    public TimeTableResponseDto setVisibility(Long memberId, Long timeTableId, TimeTableVisibilityUpdateRequestDto visibilityUpdateRequestDto) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다."));

        validateOwner(timeTable, memberId);

        timeTable.updateVisibility(visibilityUpdateRequestDto.visibility());
        return TimeTableResponseDto.from(timeTable);
    }


    /**
     * 대표 시간표 수정 메서드
     */
    @Transactional
    public TimeTableResponseDto setIsPrimary(Long memberId, Long timeTableId) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다."));

        validateOwner(timeTable, memberId);

        if (timeTable.isPrimary()) {
            return TimeTableResponseDto.from(timeTable);
        }

        timeTableRepository.findByMemberIdAndSemesterIdAndIsPrimaryTrue(
                timeTable.getMember().getId(),
                timeTable.getSemester().getId()
        ).ifPresent(TimeTable::unmarkPrimary);

        timeTable.markPrimary();
        return TimeTableResponseDto.from(timeTable);
    }


    /**
     * 시간표 이름 변경 메서드
     */
    @Transactional
    public TimeTableResponseDto setTimeTableName(Long memberId, Long timeTableId, TimeTableNameUpdateRequestDto nameUpdateRequestDto) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다."));

        validateOwner(timeTable, memberId);

        checkDuplicateTimeTableNameForUpdate(
                timeTable.getMember().getId(),
                timeTable.getSemester().getId(),
                nameUpdateRequestDto.timeTableName(),
                timeTableId
        );

        timeTable.updateTimeTableName(nameUpdateRequestDto.timeTableName());

        return TimeTableResponseDto.from(timeTable);
    }

    /**
     * 시간표 삭제 메서드
     */
    @Transactional
    public void deleteTimeTable(Long memberId, Long timeTableId) {
        TimeTable timeTable = timeTableRepository.findById(timeTableId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시간표입니다."));

        validateOwner(timeTable, memberId);

        timeTableItemRepository.deleteAllByTimeTableId(timeTableId);
        timeTableRepository.delete(timeTable);
    }

    /**
     * 사용자 검증 메서드
     */
    private void validateOwner(TimeTable timeTable, Long memberId) {
        if (!timeTable.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("해당 시간표에 접근할 권한이 없습니다.");
        }
    }


    /**
     * 시간표 전체 조회
     */
    public List<TimeTableResponseDto> getTimeTables(Long memberId) {
        return timeTableRepository.findAllByMemberId(memberId).stream()
                .map(TimeTableResponseDto::from)
                .toList();
    }

    /**
     * 학기별 시간표 조회(id)
     */
    public List<TimeTableResponseDto> getTimeTablesOfSemester(Long memberId, Long semesterId) {
        if (!semesterRepository.existsById(semesterId))
            throw new IllegalArgumentException("해당 학기가 존재하지 않습니다.");

        return timeTableRepository.findAllByMemberIdAndSemesterId(memberId, semesterId).stream()
                .map(TimeTableResponseDto::from)
                .toList();
    }

    /**
     * 학기별 시간표 조회 (년도+학기)
     */
    public List<TimeTableResponseDto> getTimeTablesOfYearAndTerm(Long memberId, Integer year, SemesterTerm term) {
        if (year == null || term == null) {
            throw new IllegalArgumentException("년도와 학기는 함께 입력해야 합니다.");
        }

        Long semesterId = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new IllegalArgumentException("해당 학기가 존재하지 않습니다."))
                .getId();

        return timeTableRepository.findAllByMemberIdAndSemesterId(memberId, semesterId).stream()
                .map(TimeTableResponseDto::from)
                .toList();
    }
}