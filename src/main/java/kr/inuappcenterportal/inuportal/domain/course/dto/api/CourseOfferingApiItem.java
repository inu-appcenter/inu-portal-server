package kr.inuappcenterportal.inuportal.domain.course.dto.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CourseOfferingApiItem(
        @JsonProperty("YEAR")
        String year,

        @JsonProperty("TERM_CODE")
        String termCode,

        @JsonProperty("TERM_NAME")
        String termName,

        @JsonProperty("COURSE_CODE")
        String courseCode,

        @JsonProperty("HAKSU_CODE")
        String haksuCode,

        @JsonProperty("COURSE_NM_KOR")
        String courseNameKor,

        @JsonProperty("COURSE_NM_ENG")
        String courseNameEng,

        @JsonProperty("COLLEGE_CODE")
        String collegeCode,

        @JsonProperty("COLLEGE_NAME")
        String collegeName,

        @JsonProperty("DEPT_CODE")
        String deptCode,

        @JsonProperty("DEPT_NAME")
        String deptName,

        @JsonProperty("HY_CODE")
        String hyCode,

        @JsonProperty("HY_NAME")
        String hyName,

        @JsonProperty("ISU_CODE")
        String isuCode,

        @JsonProperty("ISU_NAME")
        String isuName,

        @JsonProperty("ISU_FLD_CODE")
        String isuFldCode,

        @JsonProperty("ISU_FLD_NAME")
        String isuFldName,

        @JsonProperty("CREDIT")
        Integer credit,

        @JsonProperty("ENGLISH_YN")
        String englishYn,

        @JsonProperty("ENGLISH_CODE")
        String englishCode,

        @JsonProperty("ENGLISH_NAME")
        String englishName,

        @JsonProperty("SUUP_TYPE_CODE")
        String suupTypeCode,

        @JsonProperty("SUUP_TYPE_NAME")
        String suupTypeName,

        @JsonProperty("CNCTR_ISU_CODE")
        String cnctrIsuCode,

        @JsonProperty("CNCTR_ISU_NAME")
        String cnctrIsuName,

        @JsonProperty("HUSS_COURSE_YN")
        String hussCourseYn,

        @JsonProperty("INPT_DATE")
        String inputDate,

        @JsonProperty("MOD_DATE")
        String modifiedDate
) {
}