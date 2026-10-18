package kr.inuappcenterportal.inuportal.domain.member.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NameConsentAction {
    AGREE("동의"),
    WITHDRAW("철회");

    private final String description;
}
