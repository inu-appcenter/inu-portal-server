package kr.inuappcenterportal.inuportal.domain.suggestion.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.suggestion.model.Suggestion;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;

@Schema(description = "건의사항 상세 응답 Dto")
@Getter
@NoArgsConstructor
public class SuggestionResponse {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

    @Schema(description = "건의사항의 데이터베이스 id 값")
    private Long id;
    @Schema(description = "건의 내용")
    private String content;
    @Schema(description = "개발자 응원 메시지")
    private String cheerMessage;
    @Schema(description = "문의 유형")
    private String category;
    @Schema(description = "앱 버전")
    private String appVersion;
    @Schema(description = "OS 타입")
    private String osType;
    @Schema(description = "OS 버전")
    private String osVersion;
    @Schema(description = "기기 모델명")
    private String deviceModel;
    @Schema(description = "처리 상태")
    private String status;
    @Schema(description = "운영진 답변 내용")
    private String answerContent;
    @Schema(description = "답변 일시")
    private String answerDate;
    @Schema(description = "작성일시")
    private String createDate;
    @Schema(description = "수정일시 (답변 포함)")
    private String modifiedDate;
    @Schema(description = "작성자 id")
    private Long memberId;
    @Schema(description = "작성자 닉네임")
    private String memberNickname;

    @Builder
    private SuggestionResponse(Long id, String content, String cheerMessage, String category, String appVersion,
                                   String osType, String osVersion, String deviceModel, String status,
                                   String answerContent, String answerDate, String createDate, String modifiedDate,
                                   Long memberId, String memberNickname) {
        this.id = id;
        this.content = content;
        this.cheerMessage = cheerMessage;
        this.category = category;
        this.appVersion = appVersion;
        this.osType = osType;
        this.osVersion = osVersion;
        this.deviceModel = deviceModel;
        this.status = status;
        this.answerContent = answerContent;
        this.answerDate = answerDate;
        this.createDate = createDate;
        this.modifiedDate = modifiedDate;
        this.memberId = memberId;
        this.memberNickname = memberNickname;
    }

    public static SuggestionResponse of(Suggestion suggestion) {
        return SuggestionResponse.builder()
                .id(suggestion.getId())
                .content(suggestion.getContent())
                .cheerMessage(suggestion.getCheerMessage())
                .category(suggestion.getCategory().name())
                .appVersion(suggestion.getAppVersion())
                .osType(suggestion.getOsType())
                .osVersion(suggestion.getOsVersion())
                .deviceModel(suggestion.getDeviceModel())
                .status(suggestion.getStatus().name())
                .answerContent(suggestion.getAnswerContent())
                .answerDate(suggestion.getAnswerDate() == null ? null : suggestion.getAnswerDate().format(DATE_TIME_FORMATTER))
                .createDate(suggestion.getCreateDate().format(DATE_TIME_FORMATTER))
                .modifiedDate(suggestion.getModifiedDate().format(DATE_TIME_FORMATTER))
                .memberId(suggestion.getMember().getId())
                .memberNickname(suggestion.getMember().getNickname())
                .build();
    }
}
