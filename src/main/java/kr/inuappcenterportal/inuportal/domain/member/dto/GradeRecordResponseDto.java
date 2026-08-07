package kr.inuappcenterportal.inuportal.domain.member.dto;

import kr.inuappcenterportal.inuportal.domain.member.enums.Grade;
import kr.inuappcenterportal.inuportal.domain.member.model.GradeRecord;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;

public record GradeRecordResponseDto(
        Long id,
        Integer year,
        SemesterTerm term,
        String courseCode,
        String title,
        Integer credit,
        Grade grade,
        String isuName,
        String isuFldName,
        String note
) {
    public static GradeRecordResponseDto from(
            GradeRecord gradeRecord
    ) {
        return new GradeRecordResponseDto(
                gradeRecord.getId(),
                gradeRecord.getSemester().getYear(),
                gradeRecord.getSemester().getTerm(),
                gradeRecord.getCourseCode(),
                gradeRecord.getTitle(),
                gradeRecord.getCredit(),
                gradeRecord.getGrade(),
                gradeRecord.getIsuName().getDescription(),
                gradeRecord.getIsuFldName().getDescription(),
                gradeRecord.getNote()
        );
    }
}
