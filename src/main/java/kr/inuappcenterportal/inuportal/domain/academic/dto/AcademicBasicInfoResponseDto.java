package kr.inuappcenterportal.inuportal.domain.academic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Student basic academic info response")
public class AcademicBasicInfoResponseDto {

    @Schema(description = "Student ID")
    private String studentId;

    @Schema(description = "Korean name")
    private String koreanName;

    @Schema(description = "English name")
    private String englishName;

    @Schema(description = "Enrollment status code")
    private String enrollmentStatusCode;

    @Schema(description = "Enrollment status name")
    private String enrollmentStatusName;

    @Schema(description = "Entrance classification code")
    private String entranceClassificationCode;

    @Schema(description = "Entrance classification name")
    private String entranceClassificationName;

    @Schema(description = "Entrance type code")
    private String entranceTypeCode;

    @Schema(description = "Entrance type name")
    private String entranceTypeName;

    @Schema(description = "Entrance date", example = "2020-03-02")
    private String entranceDate;

    @Schema(description = "Latest enrollment change code")
    private String latestEnrollmentChangeCode;

    @Schema(description = "Latest enrollment change name")
    private String latestEnrollmentChangeName;

    @Schema(description = "Latest enrollment change date", example = "2024-02-29")
    private String latestEnrollmentChangeDate;

    @Schema(description = "Gender code")
    private String genderCode;

    @Schema(description = "Gender name")
    private String genderName;

    @Schema(description = "Birth date", example = "2001-01-15")
    private String birthDate;

    @Schema(description = "Department code")
    private String departmentCode;

    @Schema(description = "Department name")
    private String departmentName;

    @Schema(description = "Major code")
    private String majorCode;

    @Schema(description = "Major name")
    private String majorName;

    @Schema(description = "College group code")
    private String collegeGroupCode;

    @Schema(description = "College group name")
    private String collegeGroupName;

    @Schema(description = "College code")
    private String collegeCode;

    @Schema(description = "College name")
    private String collegeName;

    @Schema(description = "Course code")
    private String courseCode;

    @Schema(description = "Course name")
    private String courseName;

    @Schema(description = "Semester sequence code")
    private String semesterSequenceCode;

    @Schema(description = "Semester sequence name")
    private String semesterSequenceName;

    @Schema(description = "Completed semester code")
    private String completedSemesterCode;

    @Schema(description = "Completed semester name")
    private String completedSemesterName;

    @Schema(description = "Nationality code")
    private String nationalityCode;

    @Schema(description = "Nationality name")
    private String nationalityName;

    @Schema(description = "Military status code")
    private String militaryStatusCode;

    @Schema(description = "Military status name")
    private String militaryStatusName;

    @Schema(description = "Readmission Y/N")
    private String readmissionYn;

    @Schema(description = "Early graduation Y/N")
    private String earlyGraduationYn;

    @Schema(description = "Graduation expected Y/N")
    private String graduationExpectedYn;

    @Schema(description = "BCRMST connection Y/N")
    private String bcrmstConnectionYn;

    @Schema(description = "Capacity IO code")
    private String capacityIoCode;

    @Schema(description = "Capacity IO name")
    private String capacityIoName;

    @Schema(description = "Skill standard code")
    private String skillStandardCode;

    @Schema(description = "Skill standard name")
    private String skillStandardName;

    @Schema(description = "Advisor professor name")
    private String advisorProfessorName;

    @Schema(description = "Acquired credits")
    private String acquiredCredits;

    @Schema(description = "Grade average")
    private String gradeAverage;

    @Schema(description = "Completed semester count")
    private String completedSemesterCount;

    @Schema(description = "Mobile phone")
    private String mobilePhone;

    @Schema(description = "Masked resident registration number")
    private String residentRegistrationNumberMasked;

    @Builder(toBuilder = true)
    public AcademicBasicInfoResponseDto(
            String studentId,
            String koreanName,
            String englishName,
            String enrollmentStatusCode,
            String enrollmentStatusName,
            String entranceClassificationCode,
            String entranceClassificationName,
            String entranceTypeCode,
            String entranceTypeName,
            String entranceDate,
            String latestEnrollmentChangeCode,
            String latestEnrollmentChangeName,
            String latestEnrollmentChangeDate,
            String genderCode,
            String genderName,
            String birthDate,
            String departmentCode,
            String departmentName,
            String majorCode,
            String majorName,
            String collegeGroupCode,
            String collegeGroupName,
            String collegeCode,
            String collegeName,
            String courseCode,
            String courseName,
            String semesterSequenceCode,
            String semesterSequenceName,
            String completedSemesterCode,
            String completedSemesterName,
            String nationalityCode,
            String nationalityName,
            String militaryStatusCode,
            String militaryStatusName,
            String readmissionYn,
            String earlyGraduationYn,
            String graduationExpectedYn,
            String bcrmstConnectionYn,
            String capacityIoCode,
            String capacityIoName,
            String skillStandardCode,
            String skillStandardName,
            String advisorProfessorName,
            String acquiredCredits,
            String gradeAverage,
            String completedSemesterCount,
            String mobilePhone,
            String residentRegistrationNumberMasked
    ) {
        this.studentId = studentId;
        this.koreanName = koreanName;
        this.englishName = englishName;
        this.enrollmentStatusCode = enrollmentStatusCode;
        this.enrollmentStatusName = enrollmentStatusName;
        this.entranceClassificationCode = entranceClassificationCode;
        this.entranceClassificationName = entranceClassificationName;
        this.entranceTypeCode = entranceTypeCode;
        this.entranceTypeName = entranceTypeName;
        this.entranceDate = entranceDate;
        this.latestEnrollmentChangeCode = latestEnrollmentChangeCode;
        this.latestEnrollmentChangeName = latestEnrollmentChangeName;
        this.latestEnrollmentChangeDate = latestEnrollmentChangeDate;
        this.genderCode = genderCode;
        this.genderName = genderName;
        this.birthDate = birthDate;
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
        this.majorCode = majorCode;
        this.majorName = majorName;
        this.collegeGroupCode = collegeGroupCode;
        this.collegeGroupName = collegeGroupName;
        this.collegeCode = collegeCode;
        this.collegeName = collegeName;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.semesterSequenceCode = semesterSequenceCode;
        this.semesterSequenceName = semesterSequenceName;
        this.completedSemesterCode = completedSemesterCode;
        this.completedSemesterName = completedSemesterName;
        this.nationalityCode = nationalityCode;
        this.nationalityName = nationalityName;
        this.militaryStatusCode = militaryStatusCode;
        this.militaryStatusName = militaryStatusName;
        this.readmissionYn = readmissionYn;
        this.earlyGraduationYn = earlyGraduationYn;
        this.graduationExpectedYn = graduationExpectedYn;
        this.bcrmstConnectionYn = bcrmstConnectionYn;
        this.capacityIoCode = capacityIoCode;
        this.capacityIoName = capacityIoName;
        this.skillStandardCode = skillStandardCode;
        this.skillStandardName = skillStandardName;
        this.advisorProfessorName = advisorProfessorName;
        this.acquiredCredits = acquiredCredits;
        this.gradeAverage = gradeAverage;
        this.completedSemesterCount = completedSemesterCount;
        this.mobilePhone = mobilePhone;
        this.residentRegistrationNumberMasked = residentRegistrationNumberMasked;
    }
}
