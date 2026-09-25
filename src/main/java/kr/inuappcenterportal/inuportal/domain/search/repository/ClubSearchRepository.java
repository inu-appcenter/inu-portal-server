package kr.inuappcenterportal.inuportal.domain.search.repository;

import kr.inuappcenterportal.inuportal.domain.search.document.ClubDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ClubSearchRepository extends ElasticsearchRepository<ClubDocument, Long> {
}
