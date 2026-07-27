package kr.inuappcenterportal.inuportal.domain.notice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NoticeCreatedEvent {
    private final Notice notice;
}
