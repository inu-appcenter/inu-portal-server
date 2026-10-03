package kr.inuappcenterportal.inuportal.domain.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "컴포넌트 하단 딥링크 정보")
public record UiComponentLinkDto(
        @Schema(description = "버튼 라벨", example = "공지사항 전체 목록 보기")
        String label,
        @Schema(description = "웹 라우트 경로", example = "/home/notice")
        String route
) {}
