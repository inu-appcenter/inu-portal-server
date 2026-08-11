package kr.inuappcenterportal.inuportal.domain.mockRegistration.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mock_enrollment", uniqueConstraints = @UniqueConstraint(
        name = "uk_mock_enrollment_member_semester_offering",
        columnNames = {"member_id", "semester_id", "course_offering_id"}))
public class MockEnrollment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mock_enrollment_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    private MockEnrollment(Member member, Semester semester, CourseOffering courseOffering) {
        this.member = member; this.semester = semester; this.courseOffering = courseOffering;
    }
    public static MockEnrollment create(Member member, Semester semester, CourseOffering offering) {
        return new MockEnrollment(member, semester, offering);
    }
}
