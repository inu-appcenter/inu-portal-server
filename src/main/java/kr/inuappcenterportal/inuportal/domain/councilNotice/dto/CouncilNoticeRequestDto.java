package kr.inuappcenterportal.inuportal.domain.councilNotice.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "총학생회 공지사항 등록/수정 요청Dto")
@Getter
@NoArgsConstructor
public class CouncilNoticeRequestDto {
    @Schema(description = "제목",example = "제목")
    @NotBlank
    @Size(max = 255)
    private String title;

    @Schema(description = "내용",example = "내용")
    @NotBlank
    @Size(max = 2000)
    private String content;

    @Builder
    public CouncilNoticeRequestDto(String title, String content){
        this.title = title;
        this.content = content;
    }
}
