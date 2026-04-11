package kr.inuappcenterportal.inuportal.domain.notice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class DepartmentNoticeScheduleExtractResponse {

    private String status;
    private Integer count;
    private List<DepartmentNoticeScheduleExtractItem> data;
}
