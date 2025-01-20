package kr.inuappcenterportal.inuportal.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Schema(description = "동아리 리스트 응답 Dto")
@Getter
@NoArgsConstructor
public class ClubListResponseDto {

    @Schema(description = "동아리 이름",example = "PINCOM")
    private String name;
    @Schema(description = "카테고리",example = "카테고리")
    private String category;
    @Schema(description = "이미지 url")
    private String imageUrl;
    @Schema(description = "동아리 페이지 url")
    private String url;
    @Schema(description = "동아리 개인 url")
    private String homeUrl;

    @Builder
    private ClubListResponseDto(String name, String category, String imageUrl, String url, String homeUrl){
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.url = url;
        this.homeUrl = homeUrl;
    }

    public static ClubListResponseDto of(Club club){
        return ClubListResponseDto.builder()
                .name(club.getName())
                .category(club.getCategory())
                .imageUrl(club.getImageUrl())
                .url(club.getUrl())
                .homeUrl(club.getHomeUrl())
                .build();
    }
}
