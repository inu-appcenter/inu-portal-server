package kr.inuappcenterportal.inuportal.domain.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import kr.inuappcenterportal.inuportal.domain.search.document.*;
import kr.inuappcenterportal.inuportal.domain.search.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public UnifiedSearchResponseDto search(String query, SearchTab tab, int page, int size) {
        if (tab == null) {
            tab = SearchTab.ALL;
        }

        if (tab == SearchTab.ALL) {
            return searchAll(query, size > 0 ? size : 3);
        } else {
            return searchSingleTab(query, tab, PageRequest.of(Math.max(0, page - 1), size > 0 ? size : 10));
        }
    }

    private UnifiedSearchResponseDto searchAll(String query, int previewSize) {
        Pageable previewPage = PageRequest.of(0, previewSize);

        CompletableFuture<UnifiedSectionDto<NoticeSearchItemDto>> noticeFuture =
                CompletableFuture.supplyAsync(() -> searchNotices(query, previewPage));
        CompletableFuture<UnifiedSectionDto<DepartmentNoticeSearchItemDto>> deptNoticeFuture =
                CompletableFuture.supplyAsync(() -> searchDepartmentNotices(query, previewPage));
        CompletableFuture<UnifiedSectionDto<PostSearchItemDto>> postFuture =
                CompletableFuture.supplyAsync(() -> searchPosts(query, previewPage));
        CompletableFuture<UnifiedSectionDto<ScheduleSearchItemDto>> scheduleFuture =
                CompletableFuture.supplyAsync(() -> searchSchedules(query, previewPage));
        CompletableFuture<UnifiedSectionDto<DirectorySearchItemDto>> directoryFuture =
                CompletableFuture.supplyAsync(() -> searchDirectory(query, previewPage));
        CompletableFuture<UnifiedSectionDto<CourseSearchItemDto>> courseFuture =
                CompletableFuture.supplyAsync(() -> searchCourses(query, previewPage));
        CompletableFuture<UnifiedSectionDto<ClubSearchItemDto>> clubFuture =
                CompletableFuture.supplyAsync(() -> searchClubs(query, previewPage));

        CompletableFuture.allOf(noticeFuture, deptNoticeFuture, postFuture, scheduleFuture, directoryFuture, courseFuture, clubFuture).join();

        UnifiedSectionDto<NoticeSearchItemDto> notices = noticeFuture.join();
        UnifiedSectionDto<DepartmentNoticeSearchItemDto> deptNotices = deptNoticeFuture.join();
        UnifiedSectionDto<PostSearchItemDto> posts = postFuture.join();
        UnifiedSectionDto<ScheduleSearchItemDto> schedules = scheduleFuture.join();
        UnifiedSectionDto<DirectorySearchItemDto> directory = directoryFuture.join();
        UnifiedSectionDto<CourseSearchItemDto> courses = courseFuture.join();
        UnifiedSectionDto<ClubSearchItemDto> clubs = clubFuture.join();

        long totalCount = notices.getTotalCount() + deptNotices.getTotalCount() + posts.getTotalCount()
                + schedules.getTotalCount() + directory.getTotalCount() + courses.getTotalCount() + clubs.getTotalCount();

        return UnifiedSearchResponseDto.builder()
                .query(query)
                .tab(SearchTab.ALL)
                .totalCount(totalCount)
                .notices(notices)
                .departmentNotices(deptNotices)
                .posts(posts)
                .schedules(schedules)
                .directory(directory)
                .courses(courses)
                .clubs(clubs)
                .build();
    }

    private UnifiedSearchResponseDto searchSingleTab(String query, SearchTab tab, Pageable pageable) {
        UnifiedSearchResponseDto.UnifiedSearchResponseDtoBuilder builder = UnifiedSearchResponseDto.builder()
                .query(query)
                .tab(tab);

        switch (tab) {
            case NOTICE -> {
                UnifiedSectionDto<NoticeSearchItemDto> section = searchNotices(query, pageable);
                return builder.totalCount(section.getTotalCount()).notices(section).build();
            }
            case DEPT_NOTICE -> {
                UnifiedSectionDto<DepartmentNoticeSearchItemDto> section = searchDepartmentNotices(query, pageable);
                return builder.totalCount(section.getTotalCount()).departmentNotices(section).build();
            }
            case POST -> {
                UnifiedSectionDto<PostSearchItemDto> section = searchPosts(query, pageable);
                return builder.totalCount(section.getTotalCount()).posts(section).build();
            }
            case SCHEDULE -> {
                UnifiedSectionDto<ScheduleSearchItemDto> section = searchSchedules(query, pageable);
                return builder.totalCount(section.getTotalCount()).schedules(section).build();
            }
            case DIRECTORY -> {
                UnifiedSectionDto<DirectorySearchItemDto> section = searchDirectory(query, pageable);
                return builder.totalCount(section.getTotalCount()).directory(section).build();
            }
            case COURSE -> {
                UnifiedSectionDto<CourseSearchItemDto> section = searchCourses(query, pageable);
                return builder.totalCount(section.getTotalCount()).courses(section).build();
            }
            case CLUB -> {
                UnifiedSectionDto<ClubSearchItemDto> section = searchClubs(query, pageable);
                return builder.totalCount(section.getTotalCount()).clubs(section).build();
            }
            default -> {
                return builder.totalCount(0).build();
            }
        }
    }

    public UnifiedSectionDto<NoticeSearchItemDto> searchNotices(String keyword, Pageable pageable) {
        try {
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("title^3", "content^1", "writer^1.5", "category^1.2")
                            .operator(Operator.And)
                    ))
                    .withHighlightQuery(createHighlightQuery(List.of("title", "content")))
                    .withPageable(pageable)
                    .build();

            SearchHits<NoticeDocument> hits = elasticsearchOperations.search(query, NoticeDocument.class);
            List<NoticeSearchItemDto> items = hits.getSearchHits().stream().map(hit -> {
                NoticeDocument doc = hit.getContent();
                String highlightedTitle = getFirstHighlight(hit, "title", doc.getTitle());
                String snippet = getFirstHighlight(hit, "content", doc.getContent() != null && doc.getContent().length() > 100
                        ? doc.getContent().substring(0, 100) + "..." : doc.getContent());
                return NoticeSearchItemDto.builder()
                        .id(doc.getId())
                        .title(highlightedTitle)
                        .snippet(snippet)
                        .writer(doc.getWriter())
                        .category(doc.getCategory())
                        .url(doc.getUrl())
                        .createDate(doc.getCreateDate())
                        .build();
            }).collect(Collectors.toList());

            return UnifiedSectionDto.of(hits.getTotalHits(), items);
        } catch (Exception e) {
            log.error("Failed to search notices in elasticsearch: {}", e.getMessage());
            return UnifiedSectionDto.empty();
        }
    }

    public UnifiedSectionDto<DepartmentNoticeSearchItemDto> searchDepartmentNotices(String keyword, Pageable pageable) {
        try {
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("title^3", "content^1", "departmentName^2", "writer^1.5")
                            .operator(Operator.And)
                    ))
                    .withHighlightQuery(createHighlightQuery(List.of("title", "content")))
                    .withPageable(pageable)
                    .build();

            SearchHits<DepartmentNoticeDocument> hits = elasticsearchOperations.search(query, DepartmentNoticeDocument.class);
            List<DepartmentNoticeSearchItemDto> items = hits.getSearchHits().stream().map(hit -> {
                DepartmentNoticeDocument doc = hit.getContent();
                String highlightedTitle = getFirstHighlight(hit, "title", doc.getTitle());
                String snippet = getFirstHighlight(hit, "content", doc.getContent() != null && doc.getContent().length() > 100
                        ? doc.getContent().substring(0, 100) + "..." : doc.getContent());
                return DepartmentNoticeSearchItemDto.builder()
                        .id(doc.getId())
                        .department(doc.getDepartment())
                        .departmentName(doc.getDepartmentName())
                        .title(highlightedTitle)
                        .snippet(snippet)
                        .writer(doc.getWriter())
                        .url(doc.getUrl())
                        .createDate(doc.getCreateDate())
                        .build();
            }).collect(Collectors.toList());

            return UnifiedSectionDto.of(hits.getTotalHits(), items);
        } catch (Exception e) {
            log.error("Failed to search department notices in elasticsearch: {}", e.getMessage());
            return UnifiedSectionDto.empty();
        }
    }

    public UnifiedSectionDto<PostSearchItemDto> searchPosts(String keyword, Pageable pageable) {
        try {
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("title^3", "content^1", "category^1.5")
                            .operator(Operator.And)
                    ))
                    .withHighlightQuery(createHighlightQuery(List.of("title", "content")))
                    .withPageable(pageable)
                    .build();

            SearchHits<PostDocument> hits = elasticsearchOperations.search(query, PostDocument.class);
            List<PostSearchItemDto> items = hits.getSearchHits().stream().map(hit -> {
                PostDocument doc = hit.getContent();
                String highlightedTitle = getFirstHighlight(hit, "title", doc.getTitle());
                String snippet = getFirstHighlight(hit, "content", doc.getContent() != null && doc.getContent().length() > 100
                        ? doc.getContent().substring(0, 100) + "..." : doc.getContent());
                return PostSearchItemDto.builder()
                        .id(doc.getId())
                        .title(highlightedTitle)
                        .snippet(snippet)
                        .category(doc.getCategory())
                        .writer(doc.getWriter())
                        .good(doc.getGood())
                        .scrap(doc.getScrap())
                        .createDate(doc.getCreateDate())
                        .build();
            }).collect(Collectors.toList());

            return UnifiedSectionDto.of(hits.getTotalHits(), items);
        } catch (Exception e) {
            log.error("Failed to search posts in elasticsearch: {}", e.getMessage());
            return UnifiedSectionDto.empty();
        }
    }

    public UnifiedSectionDto<ScheduleSearchItemDto> searchSchedules(String keyword, Pageable pageable) {
        try {
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("content^3")
                            .operator(Operator.And)
                    ))
                    .withHighlightQuery(createHighlightQuery(List.of("content")))
                    .withPageable(pageable)
                    .build();

            SearchHits<ScheduleDocument> hits = elasticsearchOperations.search(query, ScheduleDocument.class);
            List<ScheduleSearchItemDto> items = hits.getSearchHits().stream().map(hit -> {
                ScheduleDocument doc = hit.getContent();
                String highlightedContent = getFirstHighlight(hit, "content", doc.getContent());
                return ScheduleSearchItemDto.builder()
                        .id(doc.getId())
                        .content(highlightedContent)
                        .startDate(doc.getStartDate())
                        .endDate(doc.getEndDate())
                        .department(doc.getDepartment())
                        .aiGenerated(doc.getAiGenerated())
                        .build();
            }).collect(Collectors.toList());

            return UnifiedSectionDto.of(hits.getTotalHits(), items);
        } catch (Exception e) {
            log.error("Failed to search schedules in elasticsearch: {}", e.getMessage());
            return UnifiedSectionDto.empty();
        }
    }

    public UnifiedSectionDto<DirectorySearchItemDto> searchDirectory(String keyword, Pageable pageable) {
        try {
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("name^3", "affiliation^2", "detailAffiliation^2", "duties^1.5", "position^1")
                            .operator(Operator.And)
                    ))
                    .withHighlightQuery(createHighlightQuery(List.of("name", "affiliation", "detailAffiliation", "duties")))
                    .withPageable(pageable)
                    .build();

            SearchHits<DirectoryDocument> hits = elasticsearchOperations.search(query, DirectoryDocument.class);
            List<DirectorySearchItemDto> items = hits.getSearchHits().stream().map(hit -> {
                DirectoryDocument doc = hit.getContent();
                return DirectorySearchItemDto.builder()
                        .id(doc.getId())
                        .name(getFirstHighlight(hit, "name", doc.getName()))
                        .affiliation(getFirstHighlight(hit, "affiliation", doc.getAffiliation()))
                        .detailAffiliation(getFirstHighlight(hit, "detailAffiliation", doc.getDetailAffiliation()))
                        .position(doc.getPosition())
                        .duties(getFirstHighlight(hit, "duties", doc.getDuties()))
                        .email(doc.getEmail())
                        .phoneNumber(doc.getPhoneNumber())
                        .build();
            }).collect(Collectors.toList());

            return UnifiedSectionDto.of(hits.getTotalHits(), items);
        } catch (Exception e) {
            log.error("Failed to search directory in elasticsearch: {}", e.getMessage());
            return UnifiedSectionDto.empty();
        }
    }

    public UnifiedSectionDto<CourseSearchItemDto> searchCourses(String keyword, Pageable pageable) {
        try {
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("title^3", "professor^2", "subjectNumber^2", "englishTitle^1.5")
                            .operator(Operator.And)
                    ))
                    .withHighlightQuery(createHighlightQuery(List.of("title", "professor")))
                    .withPageable(pageable)
                    .build();

            SearchHits<CourseDocument> hits = elasticsearchOperations.search(query, CourseDocument.class);
            List<CourseSearchItemDto> items = hits.getSearchHits().stream().map(hit -> {
                CourseDocument doc = hit.getContent();
                return CourseSearchItemDto.builder()
                        .id(doc.getId())
                        .subjectNumber(doc.getSubjectNumber())
                        .title(getFirstHighlight(hit, "title", doc.getTitle()))
                        .englishTitle(doc.getEnglishTitle())
                        .professor(getFirstHighlight(hit, "professor", doc.getProfessor()))
                        .credit(doc.getCredit())
                        .hyName(doc.getHyName())
                        .isuName(doc.getIsuName())
                        .build();
            }).collect(Collectors.toList());

            return UnifiedSectionDto.of(hits.getTotalHits(), items);
        } catch (Exception e) {
            log.error("Failed to search courses in elasticsearch: {}", e.getMessage());
            return UnifiedSectionDto.empty();
        }
    }

    public UnifiedSectionDto<ClubSearchItemDto> searchClubs(String keyword, Pageable pageable) {
        try {
            NativeQuery query = new NativeQueryBuilder()
                    .withQuery(q -> q.multiMatch(m -> m
                            .query(keyword)
                            .fields("name^3", "recruitContent^1", "category^1.5")
                            .operator(Operator.And)
                    ))
                    .withHighlightQuery(createHighlightQuery(List.of("name", "recruitContent")))
                    .withPageable(pageable)
                    .build();

            SearchHits<ClubDocument> hits = elasticsearchOperations.search(query, ClubDocument.class);
            List<ClubSearchItemDto> items = hits.getSearchHits().stream().map(hit -> {
                ClubDocument doc = hit.getContent();
                String snippet = getFirstHighlight(hit, "recruitContent", doc.getRecruitContent() != null && doc.getRecruitContent().length() > 100
                        ? doc.getRecruitContent().substring(0, 100) + "..." : doc.getRecruitContent());
                return ClubSearchItemDto.builder()
                        .id(doc.getId())
                        .name(getFirstHighlight(hit, "name", doc.getName()))
                        .category(doc.getCategory())
                        .snippet(snippet)
                        .build();
            }).collect(Collectors.toList());

            return UnifiedSectionDto.of(hits.getTotalHits(), items);
        } catch (Exception e) {
            log.error("Failed to search clubs in elasticsearch: {}", e.getMessage());
            return UnifiedSectionDto.empty();
        }
    }

    private org.springframework.data.elasticsearch.core.query.HighlightQuery createHighlightQuery(List<String> fieldNames) {
        HighlightParameters parameters = HighlightParameters.builder()
                .withPreTags("<mark>")
                .withPostTags("</mark>")
                .withFragmentSize(120)
                .withNumberOfFragments(1)
                .build();

        List<HighlightField> fields = fieldNames.stream()
                .map(HighlightField::new)
                .collect(Collectors.toList());

        Highlight highlight = new Highlight(parameters, fields);
        return new org.springframework.data.elasticsearch.core.query.HighlightQuery(highlight, null);
    }

    private String getFirstHighlight(SearchHit<?> hit, String fieldName, String defaultValue) {
        List<String> fragments = hit.getHighlightField(fieldName);
        if (fragments != null && !fragments.isEmpty()) {
            return fragments.get(0);
        }
        return defaultValue;
    }
}
