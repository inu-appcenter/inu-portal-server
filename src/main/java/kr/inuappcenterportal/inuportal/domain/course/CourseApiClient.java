package kr.inuappcenterportal.inuportal.domain.course;

import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseMeetingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseOfferingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.SchoolApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseApiClient {
    private final WebClient schoolApiWebClient;

    public SchoolApiResponseDto<CourseOfferingApiItem> fetchCourseOfferings(
            int year,
            String termCode,
            int page
    ) {
        return schoolApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/course-offerings")
                        .queryParam("year", year)
                        .queryParam("termCode", termCode)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SchoolApiResponseDto<CourseOfferingApiItem>>() {
                })
                .block();
    }

    public SchoolApiResponseDto<CourseMeetingApiItem> fetchCourseMeetings(
            int year,
            String termCode,
            int page
    ) {
        return schoolApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/course-meetings")
                        .queryParam("year", year)
                        .queryParam("termCode", termCode)
                        .queryParam("page", page)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SchoolApiResponseDto<CourseMeetingApiItem>>() {
                })
                .block();
    }

    public List<CourseOfferingApiItem> fetchAllCourseOfferings(int year, String termCode) {
        List<CourseOfferingApiItem> result = new ArrayList<>();

        int page = 1;

        while (true) {
            SchoolApiResponseDto<CourseOfferingApiItem> response =
                    fetchCourseOfferings(year, termCode, page);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }

            result.addAll(response.data());

            if (page >= response.totalPageSize()) {
                break;
            }

            page++;
        }

        return result;
    }

    public List<CourseMeetingApiItem> fetchAllMeetings(int year, String termCode) {
        List<CourseMeetingApiItem> result = new ArrayList<>();

        int page = 1;

        while (true) {
            SchoolApiResponseDto<CourseMeetingApiItem> response =
                    fetchCourseMeetings(year, termCode, page);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }
            result.addAll(response.data());

            if (page >= response.totalPageSize()) {
                break;
            }

            page++;
        }

        return result;
    }

}
