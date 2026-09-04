package kr.inuappcenterportal.inuportal.domain.firebase.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationSubFilter;
import kr.inuappcenterportal.inuportal.domain.firebase.enums.AdminNotificationTargetType;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;

import java.time.LocalDateTime;
import java.util.List;

// 예약 발송은 요청을 JSON으로 저장했다가 발송 시점에 역직렬화한다. 그 사이 필드가
// 추가돼도 이미 저장된 예약 건의 역직렬화가 깨지지 않도록 알 수 없는 필드를 무시한다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminNotificationRequest(

        @Schema(description = "Target type", example = "ALL")
        AdminNotificationTargetType targetType,

        @Schema(description = "Sub target filter", example = "NONE")
        AdminNotificationSubFilter subFilter,

        @Schema(description = "Target member ids", example = "[1, 2, 3]")
        List<Long> memberIds,

        @Schema(description = "Target student ids", example = "[201900001, 202000002]")
        List<String> studentIds,

        @Schema(description = "Target departments", example = "[COMPUTER_ENGINEERING, BUSINESS_ADMINISTRATION]")
        List<Department> departments,

        @Schema(description = "Notification title", example = "Survey event")
        @NotBlank(message = "Title must not be blank.")
        String title,

        @Schema(description = "Notification content", example = "Please join the survey event.")
        @NotBlank(message = "Content must not be blank.")
        String content,

        @Schema(description = "Path to navigate to when the notification is tapped. Full URL for web, relative path for in-app routes.", example = "/board/1")
        String path,

        @Schema(description = "Scheduled send time. Null means send immediately.", example = "2026-09-05T09:00:00")
        @Future(message = "Scheduled time must be in the future.")
        LocalDateTime scheduledAt

) {
    public AdminNotificationTargetType resolveTargetType() {
        if (targetType != null) {
            return targetType;
        }
        if (memberIds != null && !memberIds.isEmpty()) {
            return AdminNotificationTargetType.MEMBERS;
        }
        if (studentIds != null && !studentIds.isEmpty()) {
            return AdminNotificationTargetType.STUDENT_IDS;
        }
        if (departments != null && !departments.isEmpty()) {
            return AdminNotificationTargetType.DEPARTMENTS;
        }
        return AdminNotificationTargetType.ALL;
    }

    public AdminNotificationSubFilter resolveSubFilter() {
        return subFilter != null ? subFilter : AdminNotificationSubFilter.NONE;
    }
}
