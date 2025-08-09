package kr.inuappcenterportal.inuportal.domain.notice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(name = "department_index")
public class DepartmentCrawlerState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_key", unique = true)
    private String deptKey;

    @Column(name = "dept_index", nullable = false)
    private int deptIndex;
    
    public DepartmentCrawlerState(String key, int index) {
        this.deptKey = key;
        this.deptIndex = index;
    }

    public void updateIndex(int index) {
        this.deptIndex = index;
    }
}
