package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto;

// 가능 시간 구간. 예: { "key": "MON", "start": "10:00", "end": "11:30" }
public record AvailableRangeDto(String key, String start, String end) {
}
