package kr.inuappcenterportal.inuportal.domain.schedulePoll.exception;

import lombok.Getter;

@Getter
public class SchedulePollException extends RuntimeException {

    private final SchedulePollErrorCode errorCode;

    public SchedulePollException(SchedulePollErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
