package kr.inuappcenterportal.inuportal.domain.search.repository;

import kr.inuappcenterportal.inuportal.domain.search.document.PostDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, Long> {
    Page<PostDocument> findByCategory(String category, Pageable pageable);
}
