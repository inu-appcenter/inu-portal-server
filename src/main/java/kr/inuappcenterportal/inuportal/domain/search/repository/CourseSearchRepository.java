package kr.inuappcenterportal.inuportal.domain.search.repository;

import kr.inuappcenterportal.inuportal.domain.search.document.CourseDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CourseSearchRepository extends ElasticsearchRepository<CourseDocument, Long> {
}
