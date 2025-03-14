package kr.inuappcenterportal.inuportal.report;


import kr.inuappcenterportal.inuportal.domain.report.dto.ReportListResponseDto;
import kr.inuappcenterportal.inuportal.domain.report.dto.ReportRequestDto;
import kr.inuappcenterportal.inuportal.domain.report.model.Report;
import kr.inuappcenterportal.inuportal.domain.report.repository.ReportRepository;
import kr.inuappcenterportal.inuportal.domain.report.service.ReportService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;



    @Test
    @DisplayName("신고 테스트")
    public void report() {
        Long memberId = 1L;
        Long postId = 1L;
        ReportRequestDto dto = new ReportRequestDto("잘못된 정보","7호관은 도서관이 아닙니다.");
        Report report = Report.builder().build();
        ReflectionTestUtils.setField(report,"id",1L);
        when(reportRepository.save(any(Report.class))).thenReturn(report);
        Long reportId = reportService.saveReport(dto,postId,memberId);
        Assertions.assertThat(reportId).isEqualTo(1L);
        verify(reportRepository).save(any());
    }

    @Test
    @DisplayName("신고 목록 가져오기 테스트")
    public void getReportList(){
        int page = 1;
        Pageable pageable = PageRequest.of(0, 8);

        Report report1 = Report.builder()
                .reason("잘못된 정보")
                .comment("7호관은 도서관이 아닙니다.")
                .postId(101L)
                .memberId(1L)
                .build();

        Report report2 = Report.builder()
                .reason("잘못된 정보")
                .comment("8호관은 도서관이 아닙니다.")
                .postId(102L)
                .memberId(2L)
                .build();

        Report report3 = Report.builder()
                .reason("잘못된 정보")
                .comment("11호관은 도서관이 아닙니다.")
                .postId(105L)
                .memberId(3L)
                .build();
        ReflectionTestUtils.setField(report1,"createDate",LocalDate.now());
        ReflectionTestUtils.setField(report2,"createDate",LocalDate.now());
        ReflectionTestUtils.setField(report3,"createDate",LocalDate.now());

        List<Report> reportList = Arrays.asList(report1, report2,report3);
        Page<Report> mockPage = new PageImpl<>(reportList, pageable, reportList.size());

        when(reportRepository.findAllBy(any())).thenReturn(mockPage);
        ReportListResponseDto responseDto = reportService.getReportList(page);

        Assertions.assertThat(responseDto).isNotNull();
        Assertions.assertThat(responseDto.getPages()).isEqualTo(1);
        Assertions.assertThat(responseDto.getTotal()).isEqualTo(3);
        Assertions.assertThat(responseDto.getReports()).hasSize(3);

        verify(reportRepository).findAllBy(pageable);
    }

}
