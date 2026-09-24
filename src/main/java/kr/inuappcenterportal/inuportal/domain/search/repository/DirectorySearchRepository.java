package kr.inuappcenterportal.inuportal.domain.search.repository;

import kr.inuappcenterportal.inuportal.domain.search.document.DirectoryDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DirectorySearchRepository extends ElasticsearchRepository<DirectoryDocument, Long> {
}
