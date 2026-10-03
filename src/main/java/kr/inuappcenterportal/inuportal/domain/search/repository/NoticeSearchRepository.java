package kr.inuappcenterportal.inuportal.domain.search.repository;

import kr.inuappcenterportal.inuportal.domain.search.document.NoticeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface NoticeSearchRepository extends ElasticsearchRepository<NoticeDocument, Long> {
    Page<NoticeDocument> findByCategory(String category, Pageable pageable);
}
