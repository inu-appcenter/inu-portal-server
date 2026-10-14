package kr.inuappcenterportal.inuportal.domain.schedulePoll.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SubmissionVia {
    SUBMIT("직접 칠해서 제출"),
    ANYTIME("언제든 괜찮아요");

    private final String description;
}
