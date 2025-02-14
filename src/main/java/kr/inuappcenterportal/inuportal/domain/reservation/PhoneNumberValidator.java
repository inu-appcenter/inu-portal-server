package kr.inuappcenterportal.inuportal.domain.reservation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PhoneNumberValidator {
    // 국제번호(+82 포함) 또는 한국 휴대폰 번호 형식
    private static final String PHONE_NUMBER_REGEX = "^(\\+\\d{1,3})?1?\\d{9,10}$";
    private static final Pattern pattern = Pattern.compile(PHONE_NUMBER_REGEX);

    public static void validate(String phoneNumber) {
        Matcher matcher = pattern.matcher(phoneNumber);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("유효하지 않은 전화번호 형식입니다.");
        }
    }
}
