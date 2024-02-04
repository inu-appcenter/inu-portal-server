package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "게시글 등록/수정 요청Dto")
@Getter
@NoArgsConstructor
public class PostDto {

    @Schema(description = "제목",example = "제목")
    @NotBlank
    private String title;

    @Schema(description = "내용",example = "내용")
    @NotBlank
    private String content;

    @Schema(description = "카테고리",allowableValues = {"수강신청","도서관","대학생활","기숙사","동아리","학사","국제교류원","장학금"})
    @NotBlank
    private String category;

    @Builder
    public PostDto(String title, String content, String category){
        this.title = title;
        this.content = content;
        this.category = category;
    }


}
