package kr.inuappcenterportal.inuportal.domain.mockRegistration.dto;

import jakarta.validation.constraints.NotNull;

public record MockCourseRequest(@NotNull Long courseOfferingId) {}
