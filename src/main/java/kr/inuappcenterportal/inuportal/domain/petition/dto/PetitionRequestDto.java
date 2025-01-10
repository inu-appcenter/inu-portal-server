package kr.inuappcenterportal.inuportal.domain.petition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "청원 등록/수정 요청Dto")
@Getter
@NoArgsConstructor
public class PetitionRequestDto {

    @Schema(description = "제목",example = "제목")
    @NotBlank
    @Size(max = 255)
    private String title;

    @Schema(description = "내용",example = "내용")
    @NotBlank
    @Size(max = 2000)
    private String content;


    @Schema(description = "비밀글 여부",example = "true")
    @NotNull
    private Boolean isPrivate;

    @Builder
    public PetitionRequestDto (String title, String  content, Boolean isPrivate){
        this.title = title;
        this.content = content;
        this.isPrivate = isPrivate;
    }
}
