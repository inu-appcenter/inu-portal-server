package kr.inuappcenterportal.inuportal.domain.semester.converter;

import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SemesterTermConverterTest {

    private final SemesterTermConverter converter = new SemesterTermConverter();

    @ParameterizedTest
    @ValueSource(strings = {"SECOND", "second", "2", "2학기", "2nd", "2ND", "20"})
    @DisplayName("2학기 관련 다양한 문자열 표현을 SECOND enum으로 정상 변환한다")
    void convertSecondTerm(String input) {
        SemesterTerm result = converter.convert(input);
        assertThat(result).isEqualTo(SemesterTerm.SECOND);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FIRST", "first", "1", "1학기", "1st", "1ST", "10"})
    @DisplayName("1학기 관련 다양한 문자열 표현을 FIRST enum으로 정상 변환한다")
    void convertFirstTerm(String input) {
        SemesterTerm result = converter.convert(input);
        assertThat(result).isEqualTo(SemesterTerm.FIRST);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUMMER", "summer", "여름", "여름학기", "여름계절학기", "30"})
    @DisplayName("여름계절학기 관련 표현을 SUMMER enum으로 정상 변환한다")
    void convertSummerTerm(String input) {
        SemesterTerm result = converter.convert(input);
        assertThat(result).isEqualTo(SemesterTerm.SUMMER);
    }

    @Test
    @DisplayName("null 또는 빈 문자열은 null을 반환한다")
    void convertEmpty() {
        assertThat(converter.convert(null)).isNull();
        assertThat(converter.convert("")).isNull();
        assertThat(converter.convert("   ")).isNull();
    }
}
