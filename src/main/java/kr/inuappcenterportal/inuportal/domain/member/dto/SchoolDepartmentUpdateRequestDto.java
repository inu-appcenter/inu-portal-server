package kr.inuappcenterportal.inuportal.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record SchoolDepartmentUpdateRequestDto(@NotBlank String departmentCode) {}
