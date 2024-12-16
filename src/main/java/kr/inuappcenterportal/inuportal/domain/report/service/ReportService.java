package kr.inuappcenterportal.inuportal.domain.report.service;

import kr.inuappcenterportal.inuportal.domain.report.dto.ReportListResponseDto;
import kr.inuappcenterportal.inuportal.domain.report.dto.ReportRequestDto;
import kr.inuappcenterportal.inuportal.domain.report.model.Report;
import kr.inuappcenterportal.inuportal.domain.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    public Long saveReport(ReportRequestDto reportRequestDto, Long postId, Long memberId){
        Report report = Report.builder()
                .reason(reportRequestDto.getReason())
                .comment(reportRequestDto.getComment())
                .postId(postId)
                .memberId(memberId)
                .build();
        return reportRepository.save(report).getId();
    }

    public ReportListResponseDto getReportList(int page){
        Pageable pageable = PageRequest.of(page>0?--page:page,8);
        return ReportListResponseDto.of(reportRepository.findAllBy(pageable));
    }
}
