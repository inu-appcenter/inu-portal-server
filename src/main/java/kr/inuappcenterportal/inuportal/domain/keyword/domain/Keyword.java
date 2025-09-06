package kr.inuappcenterportal.inuportal.domain.keyword.domain;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.FcmMessageType;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String keyword;

    @Column
    private FcmMessageType type;

    @Column
    private Department department;

    @Builder
    private Keyword(Long memberId, String keyword, FcmMessageType type, Department department) {
        this.memberId = memberId;
        this.keyword = keyword;
        this.type = type;
        this.department = department;
    }
}
