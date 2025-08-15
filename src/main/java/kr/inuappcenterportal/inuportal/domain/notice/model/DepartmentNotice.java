package kr.inuappcenterportal.inuportal.domain.notice.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "department_notice")
public class DepartmentNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Department department;

    @Column(nullable = false)
    private String title;

    @Column(name="create_date", nullable = false)
    private String createDate;

    @Column(nullable = false)
    private Long view;

    @Column(nullable = false, length = 512)
    private String url;

    public DepartmentNotice(Department department, String title, String createDate, Long view, String url) {
        this.department = department;
        this.title = title;
        this.createDate = createDate;
        this.view = view;
        this.url = url;
    }
}
