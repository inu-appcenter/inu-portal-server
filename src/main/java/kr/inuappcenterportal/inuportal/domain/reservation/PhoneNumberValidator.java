package kr.inuappcenterportal.inuportal.domain.reservation;

import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberValidator {
    // 한국 휴대전화 번호 (010으로 시작, 10~11자리, 대시(-) 없이 입력)
    private static final String PHONE_NUMBER_REGEX = "^01[016789]\\d{7,8}$";
    private static final Pattern pattern = Pattern.compile(PHONE_NUMBER_REGEX);

    public static void validate(String phoneNumber) {
        Matcher matcher = pattern.matcher(phoneNumber);
        if (!matcher.matches()) {
            throw new MyException(MyErrorCode.WRONG_PHONE_NUMBER);
        }
    }
}
