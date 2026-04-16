package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto;

import java.time.OffsetDateTime;

public record InstagramMenuPost(
        String postUrl,
        String caption,
        OffsetDateTime publishedAt
) {
}
