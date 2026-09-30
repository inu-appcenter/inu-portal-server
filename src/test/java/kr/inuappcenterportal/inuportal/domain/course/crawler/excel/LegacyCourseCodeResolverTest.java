package kr.inuappcenterportal.inuportal.domain.course.crawler.excel;

import static org.assertj.core.api.Assertions.assertThat;

import kr.inuappcenterportal.inuportal.domain.department.enums.College;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import org.junit.jupiter.api.Test;

class LegacyCourseCodeResolverTest {

    @Test
    void 편람_학과명을_개설강의_필터가_쓰는_학과코드로_바꾼다() {
        // 제보 사례: 2024-2학기 도시건축학부 과목이 학과 필터에서 빠졌다.
        String code = LegacyCourseCodeResolver.deptCode("도시건축학부");

        assertThat(code).isEqualTo("0000038");
        assertThat(Department.URBAN_ARCHITECTURE.apiCodes()).contains(code);
    }

    @Test
    void 옛_학과명과_야간_학과도_현행_코드로_바꾼다() {
        assertThat(LegacyCourseCodeResolver.deptCode("신문방송학과"))
                .isEqualTo(LegacyCourseCodeResolver.deptCode("미디어커뮤니케이션학과"));
        assertThat(LegacyCourseCodeResolver.deptCode("컴퓨터공학부(야)"))
                .isEqualTo(LegacyCourseCodeResolver.deptCode("컴퓨터공학부"));
    }

    @Test
    void 단과대_코드는_College_enum과_같다() {
        for (College college : College.values()) {
            String code = LegacyCourseCodeResolver.collegeCode(college.getCollegeName());
            if (code != null) {
                assertThat(code).isEqualTo(college.getCollegeCode());
            }
        }
        assertThat(LegacyCourseCodeResolver.collegeCode("도시과학대학")).isEqualTo("0000033");
    }

    @Test
    void 학년_이수구분_이수영역_수업유형_집중이수를_코드로_바꾼다() {
        assertThat(LegacyCourseCodeResolver.hyCode("전학년")).isEqualTo("0");
        assertThat(LegacyCourseCodeResolver.isuCode("전공핵심")).isEqualTo("31");
        assertThat(LegacyCourseCodeResolver.isuFldCode("기초과학ㆍ공학")).isEqualTo("162");
        assertThat(LegacyCourseCodeResolver.isuFldCode("기초과학·공학")).isEqualTo("162");
        assertThat(LegacyCourseCodeResolver.ssupTypeCode("강의(이론)")).isEqualTo("1");
        assertThat(LegacyCourseCodeResolver.cnctrIsuCode("일반(1~15주)")).isEqualTo("0");
    }

    @Test
    void 모르는_이름과_빈_값은_null이라_호출부가_기존_코드를_유지한다() {
        assertThat(LegacyCourseCodeResolver.deptCode("인공지능·창업연계전공")).isNull();
        assertThat(LegacyCourseCodeResolver.isuCode("전공필수")).isNull();
        assertThat(LegacyCourseCodeResolver.deptCode(null)).isNull();
        assertThat(LegacyCourseCodeResolver.deptCode("  ")).isNull();
        assertThat(LegacyCourseCodeResolver.deptCode(" 도시건축학부 ")).isEqualTo("0000038");
    }
}
