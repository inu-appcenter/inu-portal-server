package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

// 셋 중 하나: {"deadlineAt": "…"} 마감 변경 / {"deadlineAt": null} 마감 해제 / {"close": true} 즉시 종료
// "deadlineAt 칸이 없음"과 "deadlineAt: null"을 구분해야 해서 record 대신 칸이 왔는지 기록하는 클래스로 받는다.
@Getter
@NoArgsConstructor
@Schema(description = "마감 변경·해제·종료 요청. deadlineAt과 close 중 하나만 보낸다")
public class SchedulePollUpdateRequestDto {

    @Schema(description = "새 마감 시각(현재 이후). null이면 마감 해제", example = "2026-10-12T23:00:00+09:00", nullable = true)
    private OffsetDateTime deadlineAt;

    @Schema(description = "true면 즉시 종료", example = "true")
    private Boolean close;

    @JsonIgnore
    private boolean deadlineAtPresent;

    @JsonSetter("deadlineAt")
    public void setDeadlineAt(OffsetDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
        this.deadlineAtPresent = true;
    }

    @JsonSetter("close")
    public void setClose(Boolean close) {
        this.close = close;
    }

    // 둘 중 정확히 하나만 왔고, close는 true일 때만 올바른 요청이다.
    @JsonIgnore
    public boolean isValidShape() {
        return close == null ? deadlineAtPresent : !deadlineAtPresent && close;
    }

    @JsonIgnore
    public boolean isCloseRequest() {
        return close != null && close;
    }
}
