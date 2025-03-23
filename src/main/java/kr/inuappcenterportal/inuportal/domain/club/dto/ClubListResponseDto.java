package kr.inuappcenterportal.inuportal.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Schema(description = "동아리 리스트 응답 Dto")
@Getter
@NoArgsConstructor
public class ClubListResponseDto {
    @Schema(description = "동아리 아이디")
    private Long id;
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
    @Schema(description = "동아리 모집 중 여부")
    private Boolean isRecruiting;

    @Builder
    private ClubListResponseDto(Long id,String name, String category, String imageUrl, String url, String homeUrl, Boolean isRecruiting){
        this.id = id;
        this.name = name;
        this.category = category;
        this.imageUrl = imageUrl;
        this.url = url;
        this.homeUrl = homeUrl;
        this.isRecruiting = isRecruiting;
    }

    public static ClubListResponseDto from(Club club){
        return ClubListResponseDto.builder()
                .id(club.getId())
                .name(club.getName())
                .category(club.getCategory())
                .imageUrl(club.getImageUrl())
                .url(club.getUrl())
                .homeUrl(club.getHomeUrl())
                .isRecruiting(club.getIsRecruiting())
                .build();
    }
}
