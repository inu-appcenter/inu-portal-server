package kr.inuappcenterportal.inuportal.domain.suggestion.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionCategory;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionStatus;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "suggestion")
public class Suggestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Column(name = "cheer_message", length = 1000)
    private String cheerMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SuggestionCategory category;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "os_type", length = 10)
    private String osType;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "device_model")
    private String deviceModel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SuggestionStatus status;

    @Column(name = "answer_content", length = 2000)
    private String answerContent;

    @Column(name = "answer_date")
    private LocalDateTime answerDate;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Builder
    public Suggestion(String content, String cheerMessage, Member member, SuggestionCategory category,
                       String appVersion, String osType, String osVersion, String deviceModel) {
        this.content = content;
        this.cheerMessage = cheerMessage;
        this.member = member;
        this.category = category;
        this.appVersion = appVersion;
        this.osType = osType;
        this.osVersion = osVersion;
        this.deviceModel = deviceModel;
        this.status = SuggestionStatus.RECEIVED;
        this.isDeleted = false;
    }

    public void changeStatus(String status) {
        try {
            this.status = SuggestionStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new MyException(MyErrorCode.WRONG_SUGGESTION_STATUS);
        }
    }

    public void registerAnswer(String answerContent, String status) {
        changeStatus(status);
        if (answerContent != null) {
            this.answerContent = answerContent;
            this.answerDate = LocalDateTime.now();
        }
    }

    public void deleteSuggestion() {
        this.isDeleted = true;
    }
}
