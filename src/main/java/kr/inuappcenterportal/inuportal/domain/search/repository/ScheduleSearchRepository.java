package kr.inuappcenterportal.inuportal.domain.search.repository;

import kr.inuappcenterportal.inuportal.domain.search.document.ScheduleDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ScheduleSearchRepository extends ElasticsearchRepository<ScheduleDocument, Long> {
}
