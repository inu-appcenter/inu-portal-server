package kr.inuappcenterportal.inuportal.member;

import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class MemberStudentIdParityRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("학번 끝자리가 홀수인 회원만 조회합니다.")
    public void findIdsByStudentIdEndingOddTest() {
        Member odd = saveMember("202001233");
        Member even = saveMember("202001234");
        Member zero = saveMember("202001230");

        List<Long> ids = memberRepository.findIdsByStudentIdEndingOdd();

        assertAll(
                () -> assertTrue(ids.contains(odd.getId())),
                () -> assertFalse(ids.contains(even.getId())),
                () -> assertFalse(ids.contains(zero.getId()))
        );
    }

    @Test
    @DisplayName("학번 끝자리가 짝수인 회원만 조회합니다. 0도 짝수로 취급합니다.")
    public void findIdsByStudentIdEndingEvenTest() {
        Member odd = saveMember("202001233");
        Member even = saveMember("202001234");
        Member zero = saveMember("202001230");

        List<Long> ids = memberRepository.findIdsByStudentIdEndingEven();

        assertAll(
                () -> assertFalse(ids.contains(odd.getId())),
                () -> assertTrue(ids.contains(even.getId())),
                () -> assertTrue(ids.contains(zero.getId()))
        );
    }

    @Test
    @DisplayName("학번 끝자리가 숫자가 아니면 홀수/짝수 어느 쪽에도 포함되지 않습니다.")
    public void nonNumericEndingIsExcludedTest() {
        Member nonNumeric = saveMember("20200123A");

        List<Long> oddIds = memberRepository.findIdsByStudentIdEndingOdd();
        List<Long> evenIds = memberRepository.findIdsByStudentIdEndingEven();

        assertAll(
                () -> assertFalse(oddIds.contains(nonNumeric.getId())),
                () -> assertFalse(evenIds.contains(nonNumeric.getId()))
        );
    }

    private Member saveMember(String studentId) {
        return memberRepository.save(Member.builder().studentId(studentId).roles(Collections.singletonList("ROLE_USER")).build());
    }
}
