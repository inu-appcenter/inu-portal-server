package kr.inuappcenterportal.inuportal.domain.customSchedule.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomSchedule extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "custom_schedule_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(nullable = false)
    private String title;

    private CustomSchedule(
            Member member,
            Semester semester,
            String title
    ) {
        this.member = member;
        this.semester = semester;
        this.title = title;
    }

    // 정적 팩토리 메서드
    public static CustomSchedule create(
            Member member,
            Semester semester,
            String title
    ) {
        return new CustomSchedule(member, semester, title);
    }

    public void setCustomScheduleTitle(String updateTitle) {
        if (updateTitle == null || updateTitle.isBlank()) {
            throw new IllegalArgumentException("커스텀 일정 이름은 필수입니다.");
        }

        this.title = updateTitle;
    }
}
