package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FreshmanLoginDto {

    @Schema(description = "수험번호", example = "202612345")
    @NotBlank(message = "수험번호를 입력해 주세요.")
    private String examNo;

    @Schema(description = "성명", example = "홍길동")
    @NotBlank(message = "성명을 입력해 주세요.")
    private String name;

    @Schema(description = "생년월일 8자리", example = "20070101")
    @NotBlank(message = "생년월일을 입력해 주세요.")
    private String birthDate;

    @Schema(description = "소속 학과명", example = "컴퓨터공학부")
    private String deptNm;

    public FreshmanLoginDto(String examNo, String name, String birthDate, String deptNm) {
        this.examNo = examNo;
        this.name = name;
        this.birthDate = birthDate;
        this.deptNm = deptNm;
    }
}
