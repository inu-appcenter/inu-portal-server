package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "횃불이 ai 그림 uri 변경 요청 Dto")
@NoArgsConstructor
@Getter
public class UriDto {
    @Schema(description = "uri")
    @NotBlank
    private String uri;

    @Builder
    private UriDto (String uri){
        this.uri = uri;
    }
}
