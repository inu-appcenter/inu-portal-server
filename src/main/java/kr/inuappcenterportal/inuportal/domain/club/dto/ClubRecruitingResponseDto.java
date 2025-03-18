package kr.inuappcenterportal.inuportal.domain.club.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "동아리 모집공고 응답Dto")
@Getter
@NoArgsConstructor
public class ClubRecruitingResponseDto {
    @Schema(description = "이미지 갯수")
    private Long imageCount;
    @Schema(description = "모집 공고")
    private String recruit;
    @Schema(description = "수정일",example = "yyyy-mm-dd")
    @DateTimeFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    private LocalDateTime modifiedDate;

    @Builder
    private ClubRecruitingResponseDto(Long imageCount, String  recruit, LocalDateTime modifiedDate){
        this.imageCount = imageCount;
        this.recruit = recruit;
        this.modifiedDate = modifiedDate;
    }

    public static ClubRecruitingResponseDto from(Club club){
        return ClubRecruitingResponseDto.builder()
                .recruit(club.getRecruit())
                .imageCount(club.getImageCount())
                .modifiedDate(club.getModifiedDate())
                .build();
    }
}
