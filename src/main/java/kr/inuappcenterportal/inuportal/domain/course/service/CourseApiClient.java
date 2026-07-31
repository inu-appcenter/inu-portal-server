package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseMeetingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseOfferingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.SchoolApiProperties;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.SchoolApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CourseApiClient {

    @Qualifier("schoolApiWebClient")
    private final WebClient schoolApiWebClient;
    private final SchoolApiProperties schoolApiProperties;

    public SchoolApiResponseDto<CourseOfferingApiItem> fetchCourseOfferings(
            int year,
            String modDate,
            int page
    ) {
        return schoolApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(schoolApiProperties.courseInfoPath())
                        .queryParam("AUTH_KEY", schoolApiProperties.authKey())
                        .queryParam("PAGE", page)
                        .queryParam("MOD_DATE", modDate)
                        .queryParam("YEAR", year)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SchoolApiResponseDto<CourseOfferingApiItem>>() {
                })
                .block();
    }

    public SchoolApiResponseDto<CourseMeetingApiItem> fetchCourseMeetings(
            int year,
            String modDate,
            int page
    ) {
        return schoolApiWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(schoolApiProperties.courseMeetingInfoPath())
                        .queryParam("AUTH_KEY", schoolApiProperties.authKey())
                        .queryParam("PAGE", page)
                        .queryParam("MOD_DATE", modDate)
                        .queryParam("YEAR", year)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SchoolApiResponseDto<CourseMeetingApiItem>>() {
                })
                .block();
    }

    public List<CourseOfferingApiItem> fetchAllCourseOfferings(int year, String modDate) {
        List<CourseOfferingApiItem> result = new ArrayList<>();

        int page = 1;

        while (true) {
            SchoolApiResponseDto<CourseOfferingApiItem> response =
                    fetchCourseOfferings(year, modDate, page);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }

            result.addAll(response.data());

            if (response.totalPageSize() == null || page >= response.totalPageSize()) {
                break;
            }

            page++;
        }

        return result;
    }

    public List<CourseMeetingApiItem> fetchAllMeetings(int year, String modDate) {
        List<CourseMeetingApiItem> result = new ArrayList<>();

        int page = 1;

        while (true) {
            SchoolApiResponseDto<CourseMeetingApiItem> response =
                    fetchCourseMeetings(year, modDate, page);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }
            result.addAll(response.data());

            if (response.totalPageSize() == null || page >= response.totalPageSize()) {
                break;
            }

            page++;
        }

        return result;
    }

}
