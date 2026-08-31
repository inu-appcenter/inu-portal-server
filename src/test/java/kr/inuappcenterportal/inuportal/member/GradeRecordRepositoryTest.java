package kr.inuappcenterportal.inuportal.member;

import jakarta.persistence.EntityManager;
import kr.inuappcenterportal.inuportal.domain.member.enums.Grade;
import kr.inuappcenterportal.inuportal.domain.member.model.GradeRecord;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.GradeRecordRepository;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class GradeRecordRepositoryTest {

    @Autowired
    private GradeRecordRepository gradeRecordRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private SemesterRepository semesterRepository;
    @Autowired
    private EntityManager em;

    private Long memberId;
    private Long semesterId;

    @BeforeEach
    void setUp() {
        Member member = memberRepository.save(Member.builder()
                .studentId("20241234")
                .roles(List.of("ROLE_USER"))
                .build());
        Semester semester = semesterRepository.save(Semester.create(
                2024,
                SemesterTerm.FIRST,
                SemesterStatus.OPEN,
                LocalDate.of(2024, 3, 2),
                LocalDate.of(2024, 6, 21)
        ));
        em.flush();

        memberId = member.getId();
        semesterId = semester.getId();
    }

    private GradeRecord newRecord(String courseCode, String title, Grade grade) {
        Member member = memberRepository.findById(memberId).orElseThrow();
        Semester semester = semesterRepository.findById(semesterId).orElseThrow();
        return GradeRecord.create(member, semester, null, courseCode, title, 3, grade, true, false);
    }

    @Test
    @DisplayName("벌크 삭제 후 동일한 (member, semester, courseCode, title) 를 재삽입해도 unique 제약에 걸리지 않는다")
    void deleteThenReinsertSameKey_noConstraintViolation() {
        gradeRecordRepository.save(newRecord("ABC123", "자료구조", Grade.A_PLUS));
        em.flush();

        gradeRecordRepository.deleteAllByMemberIdAndSemesterId(memberId, semesterId);

        assertDoesNotThrow(() -> {
            gradeRecordRepository.save(newRecord("ABC123", "자료구조", Grade.B_PLUS));
            em.flush();
        });

        List<GradeRecord> rows =
                gradeRecordRepository.findAllByMemberIdAndSemesterId(memberId, semesterId);
        assertEquals(1, rows.size());
        assertEquals(Grade.B_PLUS, rows.get(0).getGrade());
    }

    @Test
    @DisplayName("벌크 삭제는 호출 즉시 DELETE 를 실행한다 (뒤이은 flush 없이도 행이 사라진다)")
    void bulkDelete_executesImmediately() {
        gradeRecordRepository.save(newRecord("ABC123", "자료구조", Grade.A_PLUS));
        em.flush();

        gradeRecordRepository.deleteAllByMemberIdAndSemesterId(memberId, semesterId);

        Long count = em.createQuery(
                        "select count(g) from GradeRecord g where g.semester.id = :semesterId", Long.class)
                .setParameter("semesterId", semesterId)
                .getSingleResult();
        assertEquals(0L, count);
    }
}
