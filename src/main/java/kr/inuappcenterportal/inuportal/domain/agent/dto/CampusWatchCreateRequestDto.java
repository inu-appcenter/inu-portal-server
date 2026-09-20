package kr.inuappcenterportal.inuportal.domain.agent.dto;

import kr.inuappcenterportal.inuportal.domain.agent.enums.CampusWatchDomain;

public record CampusWatchCreateRequestDto(
        CampusWatchDomain domain,
        String targetId,
        String targetName,
        Integer durationMinutes
) {}
