package kr.inuappcenterportal.inuportal.domain.club.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "동아리 모집공고 응답Dto")
@Getter
@NoArgsConstructor
public class ClubRecruitingResponseDto {
    @Schema(description = "이미지 갯수")
    private Long imageCount;
    @Schema(description = "모집 공고")
    private String recruit;

    @Builder
    private ClubRecruitingResponseDto(Long imageCount, String  recruit){
        this.imageCount = imageCount;
        this.recruit = recruit;
    }

    public static ClubRecruitingResponseDto from(Club club){
        return ClubRecruitingResponseDto.builder()
                .recruit(club.getRecruit())
                .imageCount(club.getImageCount())
                .build();
    }
}
