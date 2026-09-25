package kr.inuappcenterportal.inuportal.domain.search.repository;

import kr.inuappcenterportal.inuportal.domain.search.document.DepartmentNoticeDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DepartmentNoticeSearchRepository extends ElasticsearchRepository<DepartmentNoticeDocument, Long> {
    Page<DepartmentNoticeDocument> findByDepartment(String department, Pageable pageable);
}
