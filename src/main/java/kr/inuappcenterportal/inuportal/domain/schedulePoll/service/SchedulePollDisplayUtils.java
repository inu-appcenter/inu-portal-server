package kr.inuappcenterportal.inuportal.domain.schedulePoll.service;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.response.SchedulePollColumnResponseDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.PollDay;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SchedulePollStatus;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.model.SchedulePoll;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

// 응답에 내려 줄 표시용 값 변환(이름, 학번, 열 이름, 시각, 상태)
public class SchedulePollDisplayUtils {

    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private SchedulePollDisplayUtils() {
    }

    // 표시 이름: 실명이 있으면 실명, 없으면 닉네임
    // TODO(8단계): 실명 필드가 생기면 실명을 먼저 쓴다.
    public static String displayName(Member member) {
        return member.getNickname();
    }

    // 앞 4자리 + *** + 끝 2자리 (202401001 → 2024***01). 6자리 이하면 가릴 부분이 없어 전부 가린다.
    public static String maskStudentId(String studentId) {
        if (studentId == null || studentId.length() <= 6) {
            return "***";
        }
        return studentId.substring(0, 4) + "***" + studentId.substring(studentId.length() - 2);
    }

    // 정기면 요일(월), 일회성이면 날짜(10/12 (월))
    public static List<SchedulePollColumnResponseDto> columns(SchedulePoll poll) {
        return SchedulePollSlotUtils.columnKeys(poll).stream()
                .map(key -> new SchedulePollColumnResponseDto(key, columnLabel(poll.isRecurring(), key)))
                .toList();
    }

    // 마감 시각이 지났으면 스케줄러가 처리하기 전(최대 1분)이라도 CLOSED로 보여 준다.
    public static SchedulePollStatus displayStatus(SchedulePollStatus status, LocalDateTime deadlineAt, LocalDateTime now) {
        if (status != SchedulePollStatus.CLOSED && deadlineAt != null && !now.isBefore(deadlineAt)) {
            return SchedulePollStatus.CLOSED;
        }
        return status;
    }

    public static OffsetDateTime toOffset(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZone(SEOUL).toOffsetDateTime();
    }

    public static LocalDateTime toSeoul(OffsetDateTime dateTime) {
        return dateTime.atZoneSameInstant(SEOUL).toLocalDateTime();
    }

    private static String columnLabel(boolean recurring, String key) {
        if (recurring) {
            return PollDay.valueOf(key).getLabel();
        }
        LocalDate date = LocalDate.parse(key);
        String day = PollDay.values()[date.getDayOfWeek().getValue() - 1].getLabel();
        return date.getMonthValue() + "/" + date.getDayOfMonth() + " (" + day + ")";
    }
}
