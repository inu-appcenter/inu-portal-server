package kr.inuappcenterportal.inuportal.domain.course.enums.course;

import lombok.Getter;

@Getter
public enum Language {
    KOREAN,
    ENGLISH;

    public static Language toLanguage(String englishYn) {
        return "Y".equalsIgnoreCase(englishYn)
                ? Language.ENGLISH
                : Language.KOREAN;
    }
}
