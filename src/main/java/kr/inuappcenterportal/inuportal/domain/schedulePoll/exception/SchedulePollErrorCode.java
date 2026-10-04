package kr.inuappcenterportal.inuportal.domain.schedulePoll.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 프론트엔드는 msg가 아니라 enum 이름(code)으로 오류를 구분한다. 이름을 바꾸지 말 것.
@Getter
@RequiredArgsConstructor
public enum SchedulePollErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않아요"),
    INVALID_RANGE(HttpStatus.BAD_REQUEST, "기간이나 시간 범위, 마감 시각을 다시 확인해 주세요"),
    INVALID_SLOT(HttpStatus.BAD_REQUEST, "선택한 시간이 범위를 벗어났어요"),
    TOO_MANY_TARGETS(HttpStatus.BAD_REQUEST, "한 번에 10곳까지 보낼 수 있어요"),

    NOT_HOST(HttpStatus.FORBIDDEN, "만든 사람만 할 수 있어요"),
    NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "시간을 제출한 사람만 볼 수 있어요"),
    BLOCKED(HttpStatus.FORBIDDEN, "참여할 수 없는 시간 맞추기예요"),
    NOT_SHARE_TARGET(HttpStatus.FORBIDDEN, "보낼 수 없는 대상이 있어요"),

    POLL_NOT_FOUND(HttpStatus.NOT_FOUND, "찾을 수 없는 시간 맞추기예요"),
    SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "제출한 시간이 없어요"),

    POLL_CLOSED(HttpStatus.CONFLICT, "이미 끝난 시간 맞추기예요"),
    POLL_FULL(HttpStatus.CONFLICT, "인원이 가득 찼어요"),
    SUBMISSION_CONFLICT(HttpStatus.CONFLICT, "다른 기기에서 먼저 수정했어요"),
    RESULT_STALE(HttpStatus.CONFLICT, "최신 결과가 아니에요"),
    RESULT_NOT_READY(HttpStatus.CONFLICT, "아직 결과가 없어요"),
    NO_PRIMARY_TIMETABLE(HttpStatus.CONFLICT, "시간표를 먼저 만들어 주세요"),
    ONE_TIME_NOT_SUPPORTED(HttpStatus.CONFLICT, "일회성 약속은 시간표에 추가할 수 없어요"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "잠시 후 다시 시도해 주세요");

    private final HttpStatus status;
    private final String message;
}
