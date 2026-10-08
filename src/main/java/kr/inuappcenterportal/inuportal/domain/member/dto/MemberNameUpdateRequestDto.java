package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "회원 실명 수정 요청Dto")
public record MemberNameUpdateRequestDto(
        @Schema(description = "실명", example = "홍길동")
        @NotBlank @Size(max = 20) String name
) {}
