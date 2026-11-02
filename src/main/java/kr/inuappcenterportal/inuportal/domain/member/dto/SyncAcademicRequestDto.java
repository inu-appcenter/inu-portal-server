package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SyncAcademicRequestDto {

    @Schema(description = "포털 학적 공식 실명", example = "홍길동")
    @NotBlank(message = "실명이 누락되었습니다.")
    private String realName;

    @Schema(description = "포털 학적 소속 학과 코드", example = "CSE")
    private String departmentCode;

    public SyncAcademicRequestDto(String realName, String departmentCode) {
        this.realName = realName;
        this.departmentCode = departmentCode;
    }
}
