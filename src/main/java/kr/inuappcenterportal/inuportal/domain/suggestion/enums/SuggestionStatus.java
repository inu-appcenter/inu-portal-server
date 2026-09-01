package kr.inuappcenterportal.inuportal.domain.suggestion.enums;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;

public enum SuggestionStatus {
    RECEIVED, IN_REVIEW, PLANNED, COMPLETED, ON_HOLD;

    public static SuggestionStatus from(String name) {
        try {
            return SuggestionStatus.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MyException(MyErrorCode.WRONG_SUGGESTION_STATUS);
        }
    }
}
