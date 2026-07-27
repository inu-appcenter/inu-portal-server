package kr.inuappcenterportal.inuportal.domain.course.service;

import kr.inuappcenterportal.inuportal.domain.course.crawler.base.CourseOverviewParser;
import kr.inuappcenterportal.inuportal.domain.course.crawler.base.CoursePageFetcher;
import kr.inuappcenterportal.inuportal.domain.course.crawler.base.CurriculumParser;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseCrawledItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CourseOverviewItemDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.CurriculumItemDto;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CourseCrawlerService {

    private final CourseService courseService;
    private final CoursePageFetcher coursePageFetcher;
    private final CourseOverviewParser courseOverviewParser;
    private final CurriculumParser curriculumParser;

    /**
     * 강의 생성(동기화) 메서드
     * (한 학과의 교과목개요와 교육과정을 크롤링해서, 강의명을 기준으로 합친 뒤 DB에 없는 강의만 저장하는 메서드)
     * 스케쥴러 대상 메서드이자 강의 동기화 메인 메서드
     */
    public void syncBaseCourses() {

        // 학과별로 동기화 로직 호출
        for (Department department : Department.values()) {
            try {
                // 교육과정 url이 없으면 해당 학과는 파싱 안함
                if (department.getCurriculumUrl() == null) {
                    continue;
                }

                // 크롤링,파싱 후 merge(크롤링 실패해도 내부에서 재시작)
                List<CurriculumItemDto> curriculumItems = crawlCurriculum(department);
                if (curriculumItems.isEmpty()) {
                    log.warn("교육과정 파싱 결과가 비어 있어 동기화를 건너뜁니다. department={}", department);
                    continue;
                }

                List<CourseOverviewItemDto> overviewItems = List.of();
                // 교과목개요 url이 있으면 크롤링, 없으면 교육과정으로만 동기화
                if (department.getCourseOverviewUrl() != null) {
                    try {
                        overviewItems = crawlCourseOverview(department);
                    } catch (Exception e) {
                        log.warn("교과목개요 크롤링 실패. department={}", department, e);
                    }
                }

                log.info("강의 기본 정보 파싱 완료. department={}, curriculumCount={}, overviewCount={}",
                        department.getDepartmentName(), curriculumItems.size(), overviewItems.size());

                // 파싱된 교육과정과 교과목개요를 합친다
                List<CourseCrawledItemDto> crawledCourses = mergeCrawledCourses(overviewItems, curriculumItems);
                log.info("강의 합치기 완료: department={}, mergedItem={}", department.getDepartmentName(), crawledCourses.size());


                // DB 동기화 로직 호출
                courseService.applyCrawledCourses(department, crawledCourses);

            } catch (Exception e) {
                log.warn("강의 기본 정보 동기화 실패. department={}", department, e);
            }
        }
    }


    /**
     * 교과목개요 크롤링 및 파싱 (title, content를 가져옴)
     */
    private List<CourseOverviewItemDto> crawlCourseOverview(Department department) {
        String url = department.getCourseOverviewUrl();

        Document document = coursePageFetcher.fetch(url);

        return courseOverviewParser.parse(document);
    }


    /**
     * 교육과정 크롤링 및 파싱 (targetGrade, targetTerm, completionDivision, credit, title을 가져옴)
     */
    private List<CurriculumItemDto> crawlCurriculum(Department department) {
        String url = department.getCurriculumUrl();

        Document document = coursePageFetcher.fetch(url);

        return curriculumParser.parse(document);
    }


    /**
     * 파싱한 두 Items들을 합쳐서 하나의 Item으로 변경(리스트)
     */
    private List<CourseCrawledItemDto> mergeCrawledCourses(
            List<CourseOverviewItemDto> overviewItems,
            List<CurriculumItemDto> curriculumItems
    ) {
        // 운영체제 -> CourseOverviewDto("운영체제", "본 강의는...")와 같이 강의명으로 매핑되는 Map으로 변환
        Map<String, CourseOverviewItemDto> overviewMap = toOverviewMap(overviewItems);

        return curriculumItems.stream()
                .map(curriculumItem -> {
                    // map의 key인 강의명으로 교과목개요를 찾는다.
                    CourseOverviewItemDto overviewItem = overviewMap.get(titleKey(curriculumItem.title()));

                    // 이렇게 찾은 overviewItem이 비어있으면 null, 교과목개요가 있으면 머지할때 추가
                    String content = overviewItem == null ? null : overviewItem.content();

                    // 기본 정보는 무조건 교육과정에서 가져오고, content만 교과목개요에서 매칭되면 붙임
                    return new CourseCrawledItemDto(
                            curriculumItem.title(),
                            content,
                            curriculumItem.targetGrade(),
                            curriculumItem.targetTerm(),
                            curriculumItem.completionDivision(),
                            curriculumItem.credit()
                    );
                })
                .toList();
    }


    /**
     * 교과목개요를 강의명으로 빠르게 찾기 위해 Map으로 변환하는 메서드
     * ex) 운영체제 -> CourseOverviewDto("운영체제", "본 강의는...")
     */
    private Map<String, CourseOverviewItemDto> toOverviewMap(List<CourseOverviewItemDto> overviewItems) {
        return overviewItems.stream()
                .collect(Collectors.toMap(
                        overviewItem -> titleKey(overviewItem.title()),
                        overviewItem -> overviewItem,
                        (first, second) -> first
                ));
    }

    /**
     * Map에서 매핑할 강의명 key 정규화 메서드
     * ex) 운영체제, 운영 체제 (OperatingSystem) 이렇게 파싱된 강의명이 다를 수 있음
     * 이걸 정규화하기 위해 key가 되는 강의명만 남기고 나머지 불필요한 값들을 제거하는 메서드임
     */
    private String titleKey(String title) {
        if (title == null) {
            return "";
        }

        return title
                .replaceAll("\\([^)]*\\)", "")   // 괄호 안 영문명 제거
                .replaceAll("^[1-4]학년", "")     // 앞에 붙은 학년 제거
                .replaceAll("\\s+", "")          // 모든 공백 제거
                .trim();
    }
}
