package kr.inuappcenterportal.inuportal.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Schema(description = "동아리 모집공고 등록/수정 Dto")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClubRecruitingRequestDto {
    @Schema(description = "모집 글",example = "내용")
    @NotBlank
    @Size(max = 2000)
    private String recruit;

    @Schema(description = "모집 여부")
    @NotNull
    private Boolean is_recruiting;
}
