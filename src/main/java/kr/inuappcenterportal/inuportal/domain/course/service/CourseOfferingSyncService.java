package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.crawler.excel.CourseOverviewProperties;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseMeetingApiItem;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseMeetingGroupKey;
import kr.inuappcenterportal.inuportal.domain.course.dto.api.CourseOfferingApiItem;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.department.service.SchoolDepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseOfferingSyncService {

    private final CourseApiClient courseApiClient;
    private final CourseOfferingService courseOfferingService;
    private final CourseMeetingService courseMeetingService;
    private final CourseOverviewProperties courseOverviewProperties;
    private final SchoolDepartmentService schoolDepartmentService;

    /**
     * 전체 개설 강의 동기화 메서드
     */
    public void syncCourseWithSchoolApi(int year, String modDate) {
        syncCourseOfferingsWithSchoolApi(year, modDate);
        syncCourseOfferingProfessorsFromExcel(year);
        syncCourseMeetingWithSchoolApi(year, modDate);
    }

    /**
     * 학교 API와 개설 강의 동기화 메서드
     */
    private void syncCourseOfferingsWithSchoolApi(int year, String modDate) {
        List<CourseOfferingApiItem> items =
                courseApiClient.fetchAllCourseOfferings(year, modDate);

        schoolDepartmentService.replaceFromRegularSemester(items);

        for (CourseOfferingApiItem item : items) {
            try {
                courseOfferingService.upsertCourseOfferings(item);
            } catch (Exception e) {
                log.warn("개설 강의 동기화 스킵. year={}, termCode={}, haksuCode={}, reason={}",
                        item.year(), item.termCode(), item.haksuCode(), e.getMessage());
            }
        }
    }

    /**
     * 서버에 저장된 편람 엑셀 파일에서 교수명을 동기화한다.
     */
    private void syncCourseOfferingProfessorsFromExcel(int year) {
        for (SemesterTerm term : SemesterTerm.values()) {
            Path path = courseOverviewProperties.resolvePath(year, term);
            if (!Files.isRegularFile(path)) {
                log.info("교수명 엑셀 파일 없음. year={}, term={}, path={}", year, term, path);
                continue;
            }

            try (InputStream inputStream = Files.newInputStream(path)) {
                courseOfferingService.updateProfessorsFromExcel(year, term, inputStream);

                log.info("교수명 엑셀 동기화 완료. year={}, term={}, path={}", year, term, path);
            } catch (Exception e) {
                log.warn("교수명 엑셀 동기화 실패. year={}, term={}, path={}", year, term, path, e);
            }
        }
    }

    /**
     * 개설 강의 시간 정보 동기화 메서드
     */
    private void syncCourseMeetingWithSchoolApi(int year, String modDate) {
        // 모든 강의 시간 정보를 api에서 받아온다
        List<CourseMeetingApiItem> items =
                courseApiClient.fetchAllMeetings(year, modDate);

        // 학수코드+년도+학기로 그룹키 생성
        Map<CourseMeetingGroupKey, List<CourseMeetingApiItem>> groupedByCourseOffering =
                generateGroupKey(items);

        courseMeetingService.upsertCourseMeetings(groupedByCourseOffering);
    }

    /**
     * 학수코드+년도+학기로 그룹키 생성하는 메서드
     */
    private Map<CourseMeetingGroupKey, List<CourseMeetingApiItem>> generateGroupKey(List<CourseMeetingApiItem> items) {
        // key = 2026 / 20 / 2000259001
        //  value = [
        //     화 야3 19:50~20:40,
        //     화 야4 20:45~21:35
        //  ]
        return items.stream()
                .collect(Collectors.groupingBy(item ->
                        new CourseMeetingGroupKey(
                                item.year(),
                                item.termCode(),
                                item.haksuCode()
                        )
                ));
    }
}
