package kr.inuappcenterportal.inuportal.domain.member.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 실명을 어디서 받았는지
@Getter
@RequiredArgsConstructor
public enum RealNameSource {
    MANUAL("직접 입력"),
    PORTAL_SCRAPE("포털에서 가져옴");

    private final String description;
}
