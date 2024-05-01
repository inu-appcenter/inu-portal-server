package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "횃불이 ai 그림 요청Dto")
@NoArgsConstructor
@Getter
public class FireDto {
    @Schema(description = "그림의 파라미터",example = "swimming")
    @NotBlank
    private String param;

    @Builder
    public FireDto(String param){
        this.param = param;
    }
}
