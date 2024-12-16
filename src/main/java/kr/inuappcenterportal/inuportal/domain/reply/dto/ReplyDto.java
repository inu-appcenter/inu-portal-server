package kr.inuappcenterportal.inuportal.domain.reply.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Schema(description = "댓글 등록 요청 Dto")
@Getter
@NoArgsConstructor
public class ReplyDto {
    @Schema(description = "내용",example = "내용")
    @NotBlank
    private String content;
    @Schema(description = "익명 여부",example = "true")
    @NotNull
    private Boolean anonymous;


    @Builder
    public ReplyDto(String content, boolean anonymous){
        this.content =content;
        this.anonymous =anonymous;
    }
}
