package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.ISU_FLD_NAME;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.ISU_NAME;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordSaveRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.enums.Grade;
import kr.inuappcenterportal.inuportal.domain.member.model.GradeRecord;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.GradeRecordRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GradeRecordService {

    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final GradeRecordRepository gradeRecordRepository;
    private final MemberRepository memberRepository;

    /**
     * 네 성적 조회 메서드
     */
    @Transactional(readOnly = true)
    public List<GradeRecordResponseDto> getGrade(Long memberId, int year, SemesterTerm term) {
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        List<GradeRecord> gradeRecords = gradeRecordRepository.findAllByMemberIdAndSemesterId(
                memberId, semester.getId());

        return gradeRecords.stream().map(GradeRecordResponseDto::from).toList();
    }

    /**
     * 성적 저장 및 업데이트 메서드
     */
    public List<GradeRecordResponseDto> upsertGrade(
            GradeRecordSaveRequestDto request,
            Long memberId
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.USER_NOT_FOUND));
        Semester semester = semesterRepository.findByYearAndTerm(request.year(), request.term())
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        gradeRecordRepository.deleteAllByMemberIdAndSemesterId(memberId, semester.getId());

        List<GradeRecord> records = request.records().
                stream().
                map(record -> GradeRecord.create(
                        member,
                        semester,
                        record.courseCode(),
                        record.title(),
                        record.credit(),
                        Grade.from(record.grade()),
                        ISU_NAME.from(record.isuName()),
                        ISU_FLD_NAME.from(record.isuFldName()),
                        record.note()
                )).toList();

        return gradeRecordRepository.saveAll(records).stream()
                .map(GradeRecordResponseDto::from)
                .toList();
    }
}
