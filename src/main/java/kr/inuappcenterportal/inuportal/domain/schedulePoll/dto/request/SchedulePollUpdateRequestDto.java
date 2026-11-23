package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

// 셋 중 하나: {"deadlineAt": "…"} 마감 변경 / {"deadlineAt": null} 마감 해제 / {"close": true} 즉시 종료
// 종료할 때 정한 후보를 함께 보낼 수 있다: {"close": true, "decided": {"resultVersion": 3, "candidateRank": 1}}
// "deadlineAt 칸이 없음"과 "deadlineAt: null"을 구분해야 해서 record 대신 칸이 왔는지 기록하는 클래스로 받는다.
@Getter
@NoArgsConstructor
@Schema(description = "마감 변경·해제·종료 요청. deadlineAt과 close 중 하나만 보낸다")
public class SchedulePollUpdateRequestDto {

    @Schema(description = "새 마감 시각(현재 이후). null이면 마감 해제", example = "2026-10-12T23:00:00+09:00", nullable = true)
    private OffsetDateTime deadlineAt;

    @Schema(description = "true면 즉시 종료", example = "true")
    private Boolean close;

    @Schema(description = "정한 후보. close: true와 함께만 보낼 수 있다. 없으면 종료만 한다. "
            + "이미 끝났지만 아직 정하지 않은 조율이면 정하기만 한다", nullable = true)
    private Decided decided;

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

    @JsonSetter("decided")
    public void setDecided(Decided decided) {
        this.decided = decided;
    }

    // deadlineAt과 close 중 정확히 하나만 왔고, close는 true일 때만 올바른 요청이다.
    // decided는 close: true와 함께일 때만 받고, 안의 두 값이 모두 있어야 한다.
    @JsonIgnore
    public boolean isValidShape() {
        boolean oneOf = close == null ? deadlineAtPresent : !deadlineAtPresent && close;
        if (!oneOf) {
            return false;
        }
        return decided == null || (isCloseRequest() && decided.isValid());
    }

    @JsonIgnore
    public boolean isCloseRequest() {
        return close != null && close;
    }

    public record Decided(
            @Schema(description = "정한 결과 번호. 최신 결과 번호여야 한다", example = "3")
            Integer resultVersion,
            @Schema(description = "정한 후보 순위 1~3", example = "1")
            Integer candidateRank
    ) {

        // 두 값이 모두 있는지만 본다. 최신 결과인지, 그 순위 후보가 있는지는 서비스에서 결과를 보고 판단한다.
        @JsonIgnore
        boolean isValid() {
            return resultVersion != null && candidateRank != null;
        }
    }
}
