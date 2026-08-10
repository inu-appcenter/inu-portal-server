package kr.inuappcenterportal.inuportal.domain.member.service;

import kr.inuappcenterportal.inuportal.domain.course.model.Course;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseRepository;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordSaveRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordUpdateRequestDto;
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
@Transactional(readOnly = true)
public class GradeRecordService {

    private final SemesterRepository semesterRepository;
    private final GradeRecordRepository gradeRecordRepository;
    private final MemberRepository memberRepository;
    private final CourseRepository courseRepository;


    /**
     * 내 전체 성적 조회 메서드
     */
    public List<GradeRecordResponseDto> getAllGradeRecord(Long memberId) {
        List<GradeRecord> gradeRecords = gradeRecordRepository.findAllByMemberId(memberId);

        return gradeRecords.stream().map(GradeRecordResponseDto::from).toList();
    }


    /**
     * 특정 학기의 네 성적 조회 메서드
     */
    public List<GradeRecordResponseDto> getGradeRecord(Long memberId, int year, SemesterTerm term) {
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        List<GradeRecord> gradeRecords = gradeRecordRepository.findAllByMemberIdAndSemesterId(
                memberId, semester.getId());

        return gradeRecords.stream().map(GradeRecordResponseDto::from).toList();
    }

    /**
     * 성적 저장 및 업데이트 메서드
     */
    @Transactional
    public List<GradeRecordResponseDto> replaceGradeRecord(
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
                        findCourse(record.courseCode()),
                        record.courseCode(),
                        record.title(),
                        record.credit(),
                        Grade.from(record.grade()),
                        record.isMajor(),
                        isCourseRepetition(record.isCourseRepetition())
                )).toList();

        return gradeRecordRepository.saveAll(records).stream()
                .map(GradeRecordResponseDto::from)
                .toList();
    }

    private Course findCourse(String courseCode) {
        if (courseCode == null || courseCode.isBlank()) {
            return null;
        }
        return courseRepository.findByCourseCode(courseCode)
                .orElse(null);
    }


    /**
     * 성적 개별 수정 메서드
     */
    @Transactional
    public GradeRecordResponseDto updateGradeRecord(Long memberId, Long gradeRecordId, GradeRecordUpdateRequestDto request) {
        GradeRecord gradeRecord = gradeRecordRepository.findByIdAndMemberId(gradeRecordId, memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.GRADE_RECORD_NOT_FOUND));

        gradeRecord.update(
                request.credit(),
                Grade.from(request.grade()),
                request.isMajor(),
                isCourseRepetition(request.isCourseRepetition())
        );

        return GradeRecordResponseDto.from(gradeRecord);
    }

    private boolean isCourseRepetition(String value) {
        return "재수강성적취소".equals(value);
    }

    /**
     * 특정 학기의 모든 성적 삭제 메서드
     */
    @Transactional
    public void deleteAllGradeRecord(Long memberId, int year, SemesterTerm term) {
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        gradeRecordRepository.deleteAllByMemberIdAndSemesterId(memberId, semester.getId());
    }

    /**
     * 성적 개별 삭제 메서드
     */
    @Transactional
    public void deleteGradeRecord(Long memberId, Long gradeRecordId) {

        GradeRecord gradeRecord = gradeRecordRepository.findByIdAndMemberId(gradeRecordId, memberId)
                .orElseThrow(() -> new MyException(MyErrorCode.GRADE_RECORD_NOT_FOUND));

        gradeRecordRepository.delete(gradeRecord);
    }
}
