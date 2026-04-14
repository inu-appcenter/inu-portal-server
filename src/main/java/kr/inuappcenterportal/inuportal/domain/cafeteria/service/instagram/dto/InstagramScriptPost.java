package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record InstagramScriptPost(
        String username,
        @JsonProperty("post_url") String postUrl,
        String caption,
        @JsonProperty("published_at_iso") String publishedAtIso,
        @JsonProperty("published_at_display") String publishedAtDisplay,
        @JsonProperty("like_count") Integer likeCount,
        @JsonProperty("image_url") String imageUrl,
        @JsonProperty("image_alt") String imageAlt,
        @JsonProperty("scraped_at_utc") String scrapedAtUtc
) {
}
