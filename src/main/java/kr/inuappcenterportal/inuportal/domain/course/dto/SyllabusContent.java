package kr.inuappcenterportal.inuportal.domain.course.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SyllabusContent(
        @JsonProperty("년도")
        Integer year,

        @JsonProperty("학기")
        String semester,

        @JsonProperty("출력일시")
        String printedAt,

        @JsonProperty("과목명")
        String subjectName,

        @JsonProperty("과목코드")
        String subjectCode,

        @JsonProperty("이수구분")
        String completionType,

        @JsonProperty("성적평가방법")
        String gradeEvaluationMethod,

        @JsonProperty("부복수전공절대평가여부")
        String doubleMajorAbsoluteEvaluation,

        @JsonProperty("집중이수제구분")
        String intensiveCourseType,

        @JsonProperty("전화번호")
        String phoneNumber,

        @JsonProperty("요일교시강의실")
        String scheduleAndRoom,

        @JsonProperty("면담가능시간")
        String officeHours,

        @JsonProperty("원어강의구분")
        String foreignLanguageType,

        @JsonProperty("학과")
        String department,

        @JsonProperty("학년")
        String targetGrade,

        @JsonProperty("소속")
        String affiliation,

        @JsonProperty("교수")
        String professor,

        @JsonProperty("학점")
        String credit,

        @JsonProperty("강의")
        String lectureHours,

        @JsonProperty("실습")
        String labHours,

        @JsonProperty("교과목개요및목적")
        String courseOverview,

        @JsonProperty("수업목표")
        String courseObjective,

        @JsonProperty("수업진행방법")
        String teachingMethod,

        @JsonProperty("수업방식비율")
        Map<String, Integer> teachingMethodRatio,

        @JsonProperty("기자재활용비율")
        Map<String, Integer> equipmentUsageRatio,

        @JsonProperty("학습평가방법")
        String assessmentMethod,

        @JsonProperty("성적평가비율")
        Map<String, Integer> gradingRatio,

        @JsonProperty("유의사항")
        List<String> notes,

        @JsonProperty("교재")
        Textbooks textbooks,

        @JsonProperty("주별수업계획")
        List<WeeklyPlan> weeklyPlans,

        @JsonProperty("과제")
        List<Assignment> assignments,

        @JsonProperty("장애학생학습지원")
        String disabilitySupport,

        @JsonProperty("핵심역량가중치")
        Map<String, Integer> coreCompetencyWeights,

        @JsonProperty("전공능력가중치")
        List<MajorCompetencyWeight> majorCompetencyWeights,

        @JsonProperty("_sections")
        List<Integer> sections,

        @JsonProperty("_pages")
        List<Integer> pages
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MajorCompetencyWeight(
            @JsonProperty("전공능력")
            String competency,

            @JsonProperty("가중치")
            Integer weight
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Textbooks(
            @JsonProperty("주교재")
            List<Book> main,

            @JsonProperty("참고서적")
            List<Book> reference,

            // 원본에서 이 필드만 구조화된 목록이 아니라 자유 서술(문자열)로 온다
            @JsonProperty("기타서적")
            String etc
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Book(
            @JsonProperty("교재명")
            String title,

            @JsonProperty("저자")
            String author,

            @JsonProperty("출판사")
            String publisher,

            @JsonProperty("발행년도")
            String publishedYear
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeeklyPlan(
            @JsonProperty("주차")
            Integer week,

            @JsonProperty("내용")
            String content
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Assignment(
            @JsonProperty("번호")
            Integer number,

            @JsonProperty("과제명")
            String title,

            @JsonProperty("제출일")
            String dueDate,

            @JsonProperty("목표")
            String goal,

            @JsonProperty("진행방법및유의사항")
            String instructions,

            @JsonProperty("참고자료")
            String reference
    ) {
    }
}
