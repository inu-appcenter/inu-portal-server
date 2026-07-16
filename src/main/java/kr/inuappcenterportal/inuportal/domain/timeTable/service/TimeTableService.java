package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.TimeTableRequestDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.TimeTableResponseDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.model.TimeTable;
import kr.inuappcenterportal.inuportal.domain.timeTable.repository.TimeTableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TimeTableService {

    private final TimeTableRepository timeTableRepository;
    private final MemberRepository memberRepository;
    private final SemesterRepository semesterRepository;

    /**
     * 시간표 생성 메서드
     */
    @Transactional
    public TimeTableResponseDto create(
            Long memberId,
            Long semesterId,
            TimeTableRequestDto request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 학기입니다."));

        duplicationCheck(memberId, semesterId, request);

        boolean isFirstTimeTable = !timeTableRepository.existsByMemberIdAndSemesterId(memberId, semesterId);

        TimeTable timeTable = TimeTable.create(
                request.timeTableName(),
                isFirstTimeTable,
                member,
                semester
        );

        TimeTable saved = timeTableRepository.save(timeTable);

        return TimeTableResponseDto.from(saved);
    }

    /**
     * 시간표 이름 중복 확인 메서드
     */
    private void duplicationCheck(
            Long memberId,
            Long semesterId,
            TimeTableRequestDto request
    ) {
        if (timeTableRepository.existsByMemberIdAndSemesterIdAndTimeTableName(
                memberId,
                semesterId,
                request.timeTableName()
        )) {
            throw new IllegalArgumentException("이미 같은 이름의 시간표가 존재합니다.");
        }
    }
}
