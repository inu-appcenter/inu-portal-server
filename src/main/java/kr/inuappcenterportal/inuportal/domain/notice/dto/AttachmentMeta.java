package kr.inuappcenterportal.inuportal.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공지사항 첨부파일 정보")
public record AttachmentMeta(
        @Schema(description = "파일명", example = "안내문.pdf")
        String name,
        @Schema(description = "다운로드 URL", example = "/bbs/inu/246/388671/download.do")
        String url,
        @Schema(description = "파일 확장자", example = "pdf")
        String fileType
) {
}
