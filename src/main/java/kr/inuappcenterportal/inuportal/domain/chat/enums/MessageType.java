package kr.inuappcenterportal.inuportal.domain.chat.enums;

public enum MessageType {
    TEXT,
    IMAGE,
    TIMETABLE_SHARE,
    BOT_QUESTION,
    BOT_ANSWER,
    SCHEDULE_POLL_INVITE,
    SCHEDULE_POLL_RESULT;

    // 서버만 만드는 종류. 사용자가 보낸 메시지에 이 종류가 들어오면 받아들이지 않는다.
    public boolean isServerOnly() {
        return this == BOT_ANSWER || this == SCHEDULE_POLL_INVITE || this == SCHEDULE_POLL_RESULT;
    }
}

