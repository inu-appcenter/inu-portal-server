package kr.inuappcenterportal.inuportal.domain.book.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;


@Getter
@Schema(description = "책 등록 Dto")
public class BookRegister {

    @NotBlank
    @Schema(description = "제목",example = "제목")
    private String name;

    @NotBlank
    @Schema(description = "저자",example = "저자")
    private String author;

    @Size(max = 2000)
    @NotBlank
    @Schema(description = "내용",example = "내용")
    private String content;

    @Min(value = 0)
    @Schema(description = "가격",example = "2000")
    private int price;


}

