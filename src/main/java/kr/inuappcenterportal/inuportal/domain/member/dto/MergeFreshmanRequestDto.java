package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MergeFreshmanRequestDto {

    @Schema(description = "포털 학적 공식 실명", example = "홍길동")
    @NotBlank(message = "실명이 누락되었습니다.")
    private String koreanName;

    @Schema(description = "생년월일 8자리", example = "20070101")
    @NotBlank(message = "생년월일이 누락되었습니다.")
    private String birthDate;

    @Schema(description = "소속 학과명", example = "컴퓨터공학부")
    private String departmentName;

    public MergeFreshmanRequestDto(String koreanName, String birthDate, String departmentName) {
        this.koreanName = koreanName;
        this.birthDate = birthDate;
        this.departmentName = departmentName;
    }
}
