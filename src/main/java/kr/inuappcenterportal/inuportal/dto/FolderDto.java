package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "스크랩폴더 등록/수정 요청Dto")
@Getter
@NoArgsConstructor
public class FolderDto {
    @Schema(description = "폴더명",example = "나만의 꿀팁 폴더")
    @NotBlank
    private String name;

    @Builder
    public FolderDto(String name){
        this.name = name;
    }
}
