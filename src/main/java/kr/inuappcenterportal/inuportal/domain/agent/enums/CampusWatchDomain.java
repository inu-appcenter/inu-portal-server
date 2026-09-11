package kr.inuappcenterportal.inuportal.domain.agent.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CampusWatchDomain {
    LIBRARY_SEAT("도서관 좌석/힐링존"),
    BOOK_RETURN("도서 반납"),
    STUDY_ROOM("스터디룸 취소표");

    private final String description;
}
