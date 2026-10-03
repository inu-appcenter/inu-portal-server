package kr.inuappcenterportal.inuportal.domain.agent.dto;

import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchDomain;
import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchStatus;
import kr.inuappcenterportal.inuportal.domain.agent.model.CampusWatchJob;

import java.time.LocalDateTime;

public record CampusWatchJobDto(
        Long id,
        CampusWatchDomain domain,
        String domainDescription,
        String targetId,
        String targetName,
        String conditionType,
        CampusWatchStatus status,
        String statusDescription,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime notifiedAt,
        long remainingMinutes
) {
    public static CampusWatchJobDto from(CampusWatchJob job) {
        LocalDateTime now = LocalDateTime.now();
        long remainingMins = java.time.Duration.between(now, job.getExpiresAt()).toMinutes();
        if (remainingMins < 0) remainingMins = 0;

        return new CampusWatchJobDto(
                job.getId(),
                job.getDomain(),
                job.getDomain().getDescription(),
                job.getTargetId(),
                job.getTargetName(),
                job.getConditionType(),
                job.getStatus(),
                job.getStatus().getDescription(),
                job.getCreatedAt(),
                job.getExpiresAt(),
                job.getNotifiedAt(),
                remainingMins
        );
    }
}
