package kr.inuappcenterportal.inuportal.domain.suggestion.enums;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;

public enum SuggestionCategory {
    BUG_REPORT, FEATURE_SUGGESTION, SCHOOL_INFO_ERROR, ETC, CHEER;

    public static SuggestionCategory from(String name) {
        try {
            return SuggestionCategory.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new MyException(MyErrorCode.WRONG_SUGGESTION_CATEGORY);
        }
    }
}
