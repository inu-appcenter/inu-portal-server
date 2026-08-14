package kr.inuappcenterportal.inuportal.domain.course.dto.api;

import java.util.List;

public record SchoolApiResponseDto<T>(
        String result,
        String resultMsg,
        Integer totalRecordCount,
        Integer totalPageSize,
        Integer pageRecordCount,
        Integer page,
        List<T> data
) {
}
