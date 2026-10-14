package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.DurationMode;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.PollDay;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// 형식 오류(누락, 모양, 정해진 값이 아님)는 400 VALIDATION_ERROR,
// 시간 범위·기간·마감 규칙 위반은 서비스에서 400 INVALID_RANGE
public record SchedulePollCreateRequestDto(
        @Schema(description = "제목 1~30자. 없으면 '회의 시간 맞추기'", example = "회의 시간 맞추기")
        @Size(min = 1, max = 30)
        String title,

        @Schema(description = "true 정기(요일) / false 일회성(날짜)", example = "false")
        @NotNull
        Boolean isRecurring,

        @Schema(description = "정기일 때 요일 1~7개, 중복 불가. 일회성이면 무시", example = "[\"MON\", \"WED\"]")
        List<PollDay> days,

        @Schema(description = "일회성일 때 시작일. 오늘 이후. 정기면 무시", example = "2026-10-12")
        LocalDate startDate,

        @Schema(description = "일회성일 때 종료일. 시작일 이후, 기간 최대 14일. 정기면 무시", example = "2026-10-16")
        LocalDate endDate,

        @Schema(description = "시간대 시작. 30분 단위, 09:00 이상", example = "09:00")
        @NotNull
        @Pattern(regexp = "([01]\\d|2[0-4]):[0-5]\\d")
        String rangeStart,

        @Schema(description = "시간대 끝. 30분 단위, 24:00 이하", example = "18:00")
        @NotNull
        @Pattern(regexp = "([01]\\d|2[0-4]):[0-5]\\d")
        String rangeEnd,

        @Schema(description = "FIXED 고정 길이 / AT_LEAST N분 이상", example = "FIXED")
        @NotNull
        DurationMode durationMode,

        @Schema(description = "FIXED면 30·60·90 중 하나, AT_LEAST면 30분 단위(시간대 길이 이하)", example = "60")
        @NotNull
        Integer durationMin,

        @Schema(description = "마감 시각. 현재 이후. 없으면 마감 없음", example = "2026-10-11T23:00:00+09:00")
        OffsetDateTime deadlineAt,

        @Schema(description = "채팅방에서 만들었을 때 그 방 ID. 내가 참여 중인 방이어야 함", example = "25")
        Long originChatRoomId
) {
}
