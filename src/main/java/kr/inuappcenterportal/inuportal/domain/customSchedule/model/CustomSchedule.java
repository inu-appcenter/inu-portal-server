package kr.inuappcenterportal.inuportal.domain.customSchedule.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
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

    @Column(nullable = false)
    private String title;

    private CustomSchedule(String title) {
        setCustomScheduleTitle(title);
    }

    // 정적 팩토리 메서드
    public static CustomSchedule create(
            String title
    ) {
        return new CustomSchedule(title);
    }

    public void setCustomScheduleTitle(String updateTitle) {
        if (updateTitle == null || updateTitle.isBlank()) {
            throw new MyException(MyErrorCode.NECESSARY_CUSTOM_TITLE);
        }

        this.title = updateTitle;
    }
}
