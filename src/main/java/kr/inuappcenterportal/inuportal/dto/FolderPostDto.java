package kr.inuappcenterportal.inuportal.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "스크랩폴더에 스크랩게시글 담기 요청Dto")
@Getter
@NoArgsConstructor
public class FolderPostDto {
    @Schema(description = "게시글의 데이터베이스 id값",example = "[1]")
    @NotNull
    private List<Long> postId;

    @Builder
    public FolderPostDto(List<Long> postId){
        this.postId = postId;
    }
}
