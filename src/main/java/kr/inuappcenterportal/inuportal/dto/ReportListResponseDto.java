package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Report;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

@Schema(description = "신고 리스트 응답Dto")
@Getter
@NoArgsConstructor
public class ReportListResponseDto {

    @Schema(description = "총 페이지 수")
    private Integer pages;

    @Schema(description = "총 신고 수")
    private Long total;

    @Schema(description = "신고 리스트")
    private List<ReportResponseDto> reports;

    @Builder
    private ReportListResponseDto(int pages, long total, List<ReportResponseDto> reports){
        this.pages = pages;
        this.total = total;
        this.reports = reports;
    }

    public static ReportListResponseDto of(Page<Report> reportPage){
        return ReportListResponseDto.builder()
                .pages(reportPage.getTotalPages())
                .total(reportPage.getTotalElements())
                .reports(reportPage.getContent().stream().map(ReportResponseDto::of).collect(Collectors.toList())).build();
    }
}
